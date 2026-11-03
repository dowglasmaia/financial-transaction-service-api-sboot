package com.dowglasmaia.financialtransaction.services.impl;

import com.dowglasmaia.financialtransaction.account.application.AccountService;
import com.dowglasmaia.financialtransaction.entity.TransactionTypeEnum;
import com.dowglasmaia.financialtransaction.exeptions.ObjectNotFoundExeption;
import com.dowglasmaia.financialtransaction.services.AtmWithdrawalService;
import com.dowglasmaia.financialtransaction.services.TransactionService;
import com.dowglasmaia.provider.model.WithdrawRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Service
public class AtmWithdrawalServiceImpl implements AtmWithdrawalService {

    private final AccountService accountService;
    private final TransactionService transactionService;

    @Autowired
    public AtmWithdrawalServiceImpl(AccountService accountService,
                                    TransactionService transactionService
    ){
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    /**
     * Debit + ledger row in ONE transaction. Only the caller's own account can be debited: a card withdrawal uses it
     * directly, a withdrawal that names an account must name the caller's (a foreign number is "not found", never debited).
     * The ledger row follows the debited account.
     */
    @Override
    @Transactional
    public void makeAtmWithdrawal(WithdrawRequestDTO withdrawRequest){
        log.info("Start makeAtmWithdrawal");

        String agencyId = accountService.currentAgencyId();
        log.info("AtmWithdrawal at agency: {}", agencyId);

        var account = accountService.currentAccount();

        if (isCardBasedWithdrawal(withdrawRequest)) {
            log.info("AtmWithdrawal base card");

            accountService.debitById(account.getId(), withdrawRequest.getAmount());
        } else {
            log.info("AtmWithdrawal not based card");

            if (!Objects.equals(withdrawRequest.getNumberAccount(), account.getNumber())) {
                throw new ObjectNotFoundExeption("Account not found.", HttpStatus.NOT_FOUND);
            }
            accountService.debitById(account.getId(), withdrawRequest.getAmount());
        }

        transactionService.save(TransactionTypeEnum.WITHDRAW, account.getId(), withdrawRequest.getAmount(), agencyId);
    }

    private boolean isCardBasedWithdrawal(WithdrawRequestDTO withdrawRequest){
        return withdrawRequest.getAgency() == null && withdrawRequest.getNumberAccount() == null;
    }
}
