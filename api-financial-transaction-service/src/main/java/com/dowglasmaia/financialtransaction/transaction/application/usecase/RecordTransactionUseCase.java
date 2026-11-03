package com.dowglasmaia.financialtransaction.transaction.application.usecase;

import com.dowglasmaia.financialtransaction.transaction.application.port.TransactionRepository;
import com.dowglasmaia.financialtransaction.transaction.domain.Transaction;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionStatus;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Writes a ledger row with an explicit status and returns its id (Pix reservation: PENDING). Joins the caller's transaction. */
@Slf4j
@Service
public class RecordTransactionUseCase {

    private final TransactionRepository ledger;

    public RecordTransactionUseCase(TransactionRepository ledger) {
        this.ledger = ledger;
    }

    @Transactional
    public String execute(TransactionType type, String accountId, BigDecimal amount, String agencyId, TransactionStatus status) {
        log.info("Start save Transaction");
        return ledger.save(Transaction.create(type, accountId, amount, agencyId, status, OffsetDateTime.now())).getId();
    }
}
