package com.dowglasmaia.financialtransaction.transaction.infrastructure.persistence;

import java.math.BigDecimal;

/** Result row of the aggregate statement query (sums over the period, count of the rows that match the filter). */
public record TotalsRow(BigDecimal sumIncoming, BigDecimal sumOutgoing, long matching) {
}
