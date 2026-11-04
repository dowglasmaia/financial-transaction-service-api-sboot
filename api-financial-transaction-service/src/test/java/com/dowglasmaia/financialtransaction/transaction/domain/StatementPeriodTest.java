package com.dowglasmaia.financialtransaction.transaction.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dowglasmaia.financialtransaction.shared.error.UnprocessableEntityException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class StatementPeriodTest {

    @Test
    void aSingleDayIsHalfOpenInTheSaoPauloZone() {
        StatementPeriod day = StatementPeriod.ofDays(LocalDate.of(2026, 3, 11), LocalDate.of(2026, 3, 11));

        // Sao Paulo is UTC-3: the 11th starts at 03:00Z and ends (exclusive) at 03:00Z of the 12th
        assertThat(day.start()).isEqualTo(OffsetDateTime.parse("2026-03-11T00:00:00-03:00"));
        assertThat(day.endExclusive()).isEqualTo(OffsetDateTime.parse("2026-03-12T00:00:00-03:00"));
    }

    @Test
    void aRangeEndsAfterTheLastDay() {
        StatementPeriod range = StatementPeriod.ofDays(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12));

        assertThat(range.endExclusive()).isEqualTo(OffsetDateTime.parse("2026-03-13T00:00:00-03:00"));
        assertThat(range.days()).containsExactly(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 11), LocalDate.of(2026, 3, 12));
    }

    @Test
    void aReversedRangeIsRejectedWithTheKnownMessage() {
        assertThatThrownBy(() -> StatementPeriod.ofDays(LocalDate.of(2026, 3, 12), LocalDate.of(2026, 3, 10)))
              .isInstanceOf(UnprocessableEntityException.class).hasMessage("The start date must not be after the end date.");
    }

    @Test
    void aMissingDateIsRejectedWithTheKnownMessage() {
        assertThatThrownBy(() -> StatementPeriod.ofDays(null, LocalDate.of(2026, 3, 10)))
              .isInstanceOf(UnprocessableEntityException.class).hasMessage("Dates are required: send from and to.");
        assertThatThrownBy(() -> StatementPeriod.ofDays(LocalDate.of(2026, 3, 10), null))
              .isInstanceOf(UnprocessableEntityException.class).hasMessage("Dates are required: send from and to.");
    }
}
