package com.dowglasmaia.financialtransaction.transaction.domain;

import java.math.BigDecimal;
import java.util.List;

/** Value object: the completed entries of a period and the account balance at the time of the query. */
public record Statement(List<Transaction> transactions, BigDecimal balance) {
}
