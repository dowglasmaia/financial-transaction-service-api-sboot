package com.dowglasmaia.exactbank.services;

import java.math.BigDecimal;

public interface TransactionService {

    void save(Object transaction, String accountId, BigDecimal amount, String agencyId);

}
