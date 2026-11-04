package com.dowglasmaia.financialtransaction.transaction.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Totals of a period plus one bucket per day (days without activity are there, with zeros). */
public record TransactionsSummary(BigDecimal sumIncoming, BigDecimal sumOutgoing, BigDecimal balance, List<DailyTotal> series) {

    public static TransactionsSummary of(StatementPeriod period, StatementTotals totals, List<DailyTotal> found, BigDecimal balance) {
        Map<LocalDate, DailyTotal> byDay = found.stream().collect(Collectors.toMap(DailyTotal::date, Function.identity()));
        List<DailyTotal> series = period.days().stream()
              .map(day -> byDay.getOrDefault(day, new DailyTotal(day, BigDecimal.ZERO, BigDecimal.ZERO)))
              .toList();
        return new TransactionsSummary(totals.sumIncoming(), totals.sumOutgoing(), balance, series);
    }
}
