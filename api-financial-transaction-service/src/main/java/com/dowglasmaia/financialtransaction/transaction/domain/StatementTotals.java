package com.dowglasmaia.financialtransaction.transaction.domain;

import java.math.BigDecimal;

/**
 * Sums of the whole period (never of one page, never narrowed by the type filter) and how many completed rows match the
 * query (this count follows the type filter and drives the paging).
 */
public record StatementTotals(BigDecimal sumIncoming, BigDecimal sumOutgoing, long matching) {
}
