package com.dowglasmaia.financialtransaction.services;

import com.dowglasmaia.financialtransaction.entity.TransactionStatusEnum;
import com.dowglasmaia.financialtransaction.entity.TransactionTypeEnum;
import com.dowglasmaia.provider.model.TransactionResponseDTO;
import com.dowglasmaia.provider.model.TransactionsResponseDTO;

import java.math.BigDecimal;

public interface TransactionService {

    /** Writes a COMPLETED ledger row. */
    void save(TransactionTypeEnum type, String accountId, BigDecimal amount, String agencyId);

    /** Writes a ledger row with an explicit status and returns its id (Pix reservation: PENDING). */
    String record(TransactionTypeEnum type, String accountId, BigDecimal amount, String agencyId, TransactionStatusEnum status);

    void updateStatus(String id, TransactionStatusEnum status);

    TransactionsResponseDTO findByDateRange(String initialDate, String finalDate);

    TransactionResponseDTO findById(String id);

}
