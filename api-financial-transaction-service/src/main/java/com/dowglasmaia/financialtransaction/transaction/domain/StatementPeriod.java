package com.dowglasmaia.financialtransaction.transaction.domain;

import com.dowglasmaia.financialtransaction.shared.error.UnprocessableEntityException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;

/**
 * Whole days, inclusive at both ends, read in the bank's zone (America/Sao_Paulo). As instants the period is half-open:
 * [first day 00:00, last day + 1 00:00). A single day (from == to) is a valid period.
 */
public record StatementPeriod(LocalDate from, LocalDate to) {

    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    public static StatementPeriod ofDays(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new UnprocessableEntityException("Dates are required: send from and to.");
        }
        if (from.isAfter(to)) {
            throw new UnprocessableEntityException("The start date must not be after the end date.");
        }
        return new StatementPeriod(from, to);
    }

    public OffsetDateTime start() {
        return from.atStartOfDay(ZONE).toOffsetDateTime();
    }

    public OffsetDateTime endExclusive() {
        return to.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime();
    }

    public List<LocalDate> days() {
        return Stream.iterate(from, day -> !day.isAfter(to), day -> day.plusDays(1)).toList();
    }
}
