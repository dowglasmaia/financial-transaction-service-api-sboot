package com.dowglasmaia.financialtransaction.transaction.application.usecase;

import com.dowglasmaia.financialtransaction.account.application.usecase.DebitAccountUseCase;
import com.dowglasmaia.financialtransaction.account.application.usecase.GetCurrentAccountUseCase;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.transaction.application.port.RechargeEventPublisher;
import com.dowglasmaia.financialtransaction.transaction.domain.MobileRecharge;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import com.dowglasmaia.financialtransaction.shared.error.UnprocessableEntityExeption;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestMobileRechargeUseCase {

    private final GetCurrentAccountUseCase getCurrentAccount;
    private final DebitAccountUseCase debitAccount;
    private final SaveTransactionUseCase saveTransaction;
    private final RechargeEventPublisher publisher;

    public RequestMobileRechargeUseCase(GetCurrentAccountUseCase getCurrentAccount,
                                        DebitAccountUseCase debitAccount,
                                        SaveTransactionUseCase saveTransaction,
                                        RechargeEventPublisher publisher) {
        this.getCurrentAccount = getCurrentAccount;
        this.debitAccount = debitAccount;
        this.saveTransaction = saveTransaction;
        this.publisher = publisher;
    }

    /**
     * Debit, ledger row and the Kafka event (as an outbox row) are written in ONE transaction; the outbox relay
     * publishes the event afterwards (at-least-once). A failure anywhere rolls everything back.
     */
    @Transactional
    public void execute(MobileRecharge recharge) {
        if (!recharge.hasValidPhone()) {
            throw new UnprocessableEntityExeption("Invalid cell phone numer");
        }

        Account account = getCurrentAccount.execute();

        debitAccount.execute(account.getId(), recharge.amount());

        saveTransaction.execute(TransactionType.PHONE_RECHARGE, account.getId(), recharge.amount(), null);

        publisher.publish(recharge);
    }
}
