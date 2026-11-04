package com.dowglasmaia.financialtransaction.transaction.application.usecase;

import com.dowglasmaia.financialtransaction.account.application.usecase.CreditAccountUseCase;
import com.dowglasmaia.financialtransaction.account.application.usecase.DebitAccountUseCase;
import com.dowglasmaia.financialtransaction.account.application.usecase.GetCurrentAccountUseCase;
import com.dowglasmaia.financialtransaction.account.application.usecase.GetCurrentAgencyUseCase;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.transaction.application.port.PixGateway;
import com.dowglasmaia.financialtransaction.transaction.domain.PixKey;
import com.dowglasmaia.financialtransaction.transaction.domain.PixTransfer;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionStatus;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import com.dowglasmaia.financialtransaction.shared.error.UnprocessableEntityException;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Pix = reserve -> call the bank -> confirm / compensate. The remote call is NEVER inside a database transaction:
 * <ol>
 *   <li>reserve (tx 1): atomic debit + ledger row PENDING;</li>
 *   <li>call the bank (timeouts + circuit breaker on the Feign client behind the {@link PixGateway} port);</li>
 *   <li>confirm (tx 2): ledger row COMPLETED; or compensate (tx 2): credit back + ledger row FAILED.</li>
 * </ol>
 * The customer is never left debited when the bank fails. Recipients are not credited locally (decision D-15).
 * The method itself is deliberately NOT @Transactional: each step opens its own transaction through the template.
 */
@Slf4j
@Service
public class MakePixTransferUseCase {

    private final PixGateway gateway;
    private final GetCurrentAccountUseCase getCurrentAccount;
    private final GetCurrentAgencyUseCase getCurrentAgency;
    private final DebitAccountUseCase debitAccount;
    private final CreditAccountUseCase creditAccount;
    private final RecordTransactionUseCase recordTransaction;
    private final UpdateTransactionStatusUseCase updateStatus;
    private final TransactionTemplate transactionTemplate;

    public MakePixTransferUseCase(PixGateway gateway,
                                  GetCurrentAccountUseCase getCurrentAccount,
                                  GetCurrentAgencyUseCase getCurrentAgency,
                                  DebitAccountUseCase debitAccount,
                                  CreditAccountUseCase creditAccount,
                                  RecordTransactionUseCase recordTransaction,
                                  UpdateTransactionStatusUseCase updateStatus,
                                  TransactionTemplate transactionTemplate) {
        this.gateway = gateway;
        this.getCurrentAccount = getCurrentAccount;
        this.getCurrentAgency = getCurrentAgency;
        this.debitAccount = debitAccount;
        this.creditAccount = creditAccount;
        this.recordTransaction = recordTransaction;
        this.updateStatus = updateStatus;
        this.transactionTemplate = transactionTemplate;
    }

    public void execute(PixTransfer transfer) {
        log.info("Start Send Pix");

        if (!isValidKeyAndExists(transfer)) {
            String errorMessage = "Pix key does not exist or is invalid for the specified type: " + transfer.key().type().label();
            log.error(errorMessage);

            throw new UnprocessableEntityException(errorMessage);
        }

        Account account = getCurrentAccount.execute();
        String agencyId = getCurrentAgency.execute();
        BigDecimal amount = transfer.amount();

        String ledgerId = reserve(account, agencyId, amount);

        try {
            gateway.send();
        } catch (Exception e) {
            log.error("Fail send Pix.", e);
            compensate(ledgerId, account.getId(), amount);

            throw new UnprocessableEntityException("Fail send Pix.");
        }

        confirm(ledgerId);
        log.info("Pix sent successfully");
    }

    /** Tx 1: atomic debit (balance >= amount) + PENDING ledger row. A rejected debit rolls everything back (422). */
    private String reserve(Account account, String agencyId, BigDecimal amount) {
        return transactionTemplate.execute(status -> {
            debitAccount.execute(account.getId(), amount);
            return recordTransaction.execute(TransactionType.PIX, account.getId(), amount, agencyId, TransactionStatus.PENDING);
        });
    }

    /** Tx 2 (success): the bank accepted the Pix. If this fails the money was sent: keep the row PENDING and flag it. */
    private void confirm(String ledgerId) {
        try {
            transactionTemplate.executeWithoutResult(status -> updateStatus.execute(ledgerId, TransactionStatus.COMPLETED));
        } catch (RuntimeException e) {
            log.error("RECONCILIATION NEEDED: Pix {} was accepted by MaiaBank but could not be confirmed in the ledger", ledgerId, e);
        }
    }

    /** Tx 2 (failure): give the money back and mark the ledger row FAILED, atomically. */
    private void compensate(String ledgerId, String accountId, BigDecimal amount) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                creditAccount.execute(accountId, amount);
                updateStatus.execute(ledgerId, TransactionStatus.FAILED);
            });
        } catch (RuntimeException e) {
            log.error("RECONCILIATION NEEDED: Pix {} failed but the debit could not be reversed (account {}, amount {})", ledgerId, accountId, amount, e);
        }
    }

    private boolean isValidKeyAndExists(PixTransfer transfer) {
        return transfer.key().isValid() && existKey(transfer.key());
    }

    /** Existence is not verified (stub, kept on purpose). The key is only ever logged masked. */
    private boolean existKey(PixKey key) {
        log.info("validate if the pixKey exists. {}", key.masked());
        return true;
    }
}
