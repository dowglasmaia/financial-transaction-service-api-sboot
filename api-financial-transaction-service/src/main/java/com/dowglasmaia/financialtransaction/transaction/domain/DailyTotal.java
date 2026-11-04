package com.dowglasmaia.financialtransaction.transaction.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Incoming and outgoing money of one Sao Paulo day. */
public record DailyTotal(LocalDate date, BigDecimal incoming, BigDecimal outgoing) {
}
