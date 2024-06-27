package com.dowglasmaia.financialtransaction.services;

import com.dowglasmaia.provider.model.TransactionResponseDTO;
import com.dowglasmaia.provider.model.TransactionsResponseDTO;

import java.math.BigDecimal;

public interface TransactionService {

    void save(Object transaction, String accountId, BigDecimal amount, String agencyId);

    TransactionsResponseDTO findByDateRange(String initialDate, String finalDate);

    TransactionResponseDTO findById(String id);

}
