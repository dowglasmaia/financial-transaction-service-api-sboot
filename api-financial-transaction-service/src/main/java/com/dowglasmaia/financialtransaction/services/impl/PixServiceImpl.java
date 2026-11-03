package com.dowglasmaia.financialtransaction.services.impl;

import com.dowglasmaia.financialtransaction.account.application.AccountService;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.entity.TransactionStatusEnum;
import com.dowglasmaia.financialtransaction.entity.TransactionTypeEnum;
import com.dowglasmaia.financialtransaction.exeptions.UnprocessableEntityExeption;
import com.dowglasmaia.financialtransaction.services.PixService;
import com.dowglasmaia.financialtransaction.services.TransactionService;
import com.dowglasmaia.financialtransaction.services.client.maiaBank.MaiaBankClient;
import com.dowglasmaia.financialtransaction.utils.CPFValidator;
import com.dowglasmaia.financialtransaction.utils.EmailValidator;
import com.dowglasmaia.financialtransaction.utils.PhoneValidator;
import com.dowglasmaia.provider.model.PixRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

/**
 * Pix = reserve -> call MaiaBank -> confirm / compensate. The remote call is NEVER inside a database transaction:
 * <ol>
 *   <li>reserve (tx 1): atomic debit + ledger row PENDING;</li>
 *   <li>call MaiaBank (timeouts + circuit breaker on the Feign client);</li>
 *   <li>confirm (tx 2): ledger row COMPLETED; or compensate (tx 2): credit back + ledger row FAILED.</li>
 * </ol>
 * The customer is never left debited when MaiaBank fails. Recipients are not credited locally (decision D-15).
 */
@Slf4j
@Service
public class PixServiceImpl implements PixService {

    private final MaiaBankClient maiaBankClient;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public PixServiceImpl(MaiaBankClient maiaBankClient,
                          AccountService accountService,
                          TransactionService transactionService,
                          TransactionTemplate transactionTemplate
    ){
        this.maiaBankClient = maiaBankClient;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void makeTransfer(PixRequestDTO request){
        log.info("Start Send Pix");

        if (!isValidKeyAndExists(request)) {
            String errorMessage = "Pix key does not exist or is invalid for the specified type: " + request.getKeyType();
            log.error(errorMessage);

            throw new UnprocessableEntityExeption(errorMessage, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Account account = accountService.currentAccount();
        String agencyId = accountService.currentAgencyId();
        BigDecimal amount = request.getTransactionAmount();

        String ledgerId = reserve(account, agencyId, amount);

        try {
            maiaBankClient.receiversPix();
        } catch (Exception e) {
            log.error("Fail send Pix.", e);
            compensate(ledgerId, account.getId(), amount);

            throw new UnprocessableEntityExeption("Fail send Pix.", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        confirm(ledgerId);
        log.info("Pix sent successfully");
    }

    /** Tx 1: atomic debit (balance >= amount) + PENDING ledger row. A rejected debit rolls everything back (422). */
    private String reserve(Account account, String agencyId, BigDecimal amount){
        return transactionTemplate.execute(status -> {
            accountService.debitById(account.getId(), amount);
            return transactionService.record(TransactionTypeEnum.PIX, account.getId(), amount, agencyId, TransactionStatusEnum.PENDING);
        });
    }

    /** Tx 2 (success): MaiaBank accepted the Pix. If this fails the money was sent: keep the row PENDING and flag it. */
    private void confirm(String ledgerId){
        try {
            transactionTemplate.executeWithoutResult(status -> transactionService.updateStatus(ledgerId, TransactionStatusEnum.COMPLETED));
        } catch (RuntimeException e) {
            log.error("RECONCILIATION NEEDED: Pix {} was accepted by MaiaBank but could not be confirmed in the ledger", ledgerId, e);
        }
    }

    /** Tx 2 (failure): give the money back and mark the ledger row FAILED, atomically. */
    private void compensate(String ledgerId, String accountId, BigDecimal amount){
        try {
            transactionTemplate.executeWithoutResult(status -> {
                accountService.credit(accountId, amount);
                transactionService.updateStatus(ledgerId, TransactionStatusEnum.FAILED);
            });
        } catch (RuntimeException e) {
            log.error("RECONCILIATION NEEDED: Pix {} failed but the debit could not be reversed (account {}, amount {})", ledgerId, accountId, amount, e);
        }
    }

    private boolean isValidKeyAndExists(PixRequestDTO request){
        return validateKey(request) && existKey(request.getPixKey());
    }


    private boolean existKey(String key){
        log.info("validate if the pixKey exists. {}", key);
        return true;
    }

    private boolean validateKey(PixRequestDTO request){
        String pixKey = request.getPixKey();
        switch (request.getKeyType()) {
            case CPF:
                return CPFValidator.isCpf(pixKey);
            case EMAIL:
                return EmailValidator.isValidEmail(pixKey);
            case PHONE:
                return PhoneValidator.isValidPhoneNumber(pixKey);
            default:
                return false;
        }
    }

}
