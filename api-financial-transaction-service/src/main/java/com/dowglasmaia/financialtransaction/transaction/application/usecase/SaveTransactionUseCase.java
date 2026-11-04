package com.dowglasmaia.financialtransaction.transaction.application.usecase;

import com.dowglasmaia.financialtransaction.transaction.domain.TransactionStatus;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Writes a COMPLETED ledger row. Joins the caller's transaction. */
@Service
public class SaveTransactionUseCase {

    private final RecordTransactionUseCase recordTransaction;

    public SaveTransactionUseCase(RecordTransactionUseCase recordTransaction) {
        this.recordTransaction = recordTransaction;
    }

    @Transactional
    public void execute(TransactionType type, String accountId, BigDecimal amount, String agencyId) {
        recordTransaction.execute(type, accountId, amount, agencyId, TransactionStatus.COMPLETED);
    }
}
