package com.dowglasmaia.financialtransaction.services;

import com.dowglasmaia.financialtransaction.transaction.domain.TransactionStatus;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import com.dowglasmaia.provider.model.TransactionResponseDTO;
import com.dowglasmaia.provider.model.TransactionsResponseDTO;

import java.math.BigDecimal;

public interface TransactionService {

    /** Writes a COMPLETED ledger row. */
    void save(TransactionType type, String accountId, BigDecimal amount, String agencyId);

    /** Writes a ledger row with an explicit status and returns its id (Pix reservation: PENDING). */
    String record(TransactionType type, String accountId, BigDecimal amount, String agencyId, TransactionStatus status);

    void updateStatus(String id, TransactionStatus status);

    TransactionsResponseDTO findByDateRange(String initialDate, String finalDate);

    TransactionResponseDTO findById(String id);

}
