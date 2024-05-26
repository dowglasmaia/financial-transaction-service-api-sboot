package com.dowglasmaia.exactbank.services.client.account;

import java.math.BigDecimal;

public interface AddAmountService {
    void addAmountToAccount(String accountNumber, BigDecimal amount);
}
