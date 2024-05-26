package com.dowglasmaia.exactbank.services;

import com.dowglasmaia.provider.model.TransactionsResponseDTO;

import java.math.BigDecimal;

public interface TransactionService {

    void save(Object transaction, String accountId, BigDecimal amount, String agencyId);

    TransactionsResponseDTO findByDateRange(String initialDate, String finalDate);

}
