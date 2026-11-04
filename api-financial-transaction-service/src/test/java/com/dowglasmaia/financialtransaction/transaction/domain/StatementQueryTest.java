package com.dowglasmaia.financialtransaction.transaction.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dowglasmaia.financialtransaction.shared.error.UnprocessableEntityException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class StatementQueryTest {

    private static final StatementPeriod DAY = StatementPeriod.ofDays(LocalDate.of(2026, 3, 11), LocalDate.of(2026, 3, 11));

    @Test
    void defaultsAreNewestFirstOnPageZero() {
        StatementQuery query = StatementQuery.of(DAY, null, null, null, null, null);

        assertThat(query.type()).isNull();
        assertThat(query.sort()).isEqualTo(StatementQuery.Sort.DATE);
        assertThat(query.ascending()).isFalse();
        assertThat(query.page()).isZero();
        assertThat(query.size()).isEqualTo(20);
    }

    @Test
    void readsTheTypedValues() {
        StatementQuery query = StatementQuery.of(DAY, "PIX", "amount", "asc", 2, 50);

        assertThat(query.type()).isEqualTo(TransactionType.PIX);
        assertThat(query.sort()).isEqualTo(StatementQuery.Sort.AMOUNT);
        assertThat(query.ascending()).isTrue();
        assertThat(query.page()).isEqualTo(2);
        assertThat(query.size()).isEqualTo(50);
    }

    @Test
    void invalidValuesAreRejectedWithTheKnownMessages() {
        assertThatThrownBy(() -> StatementQuery.of(DAY, "LOAN", null, null, null, null))
              .isInstanceOf(UnprocessableEntityException.class).hasMessage("type must be one of: DEPOSIT, PIX, PHONE_RECHARGE, WITHDRAW");
        assertThatThrownBy(() -> StatementQuery.of(DAY, null, "name", null, null, null))
              .isInstanceOf(UnprocessableEntityException.class).hasMessage("sort must be one of: date, amount");
        assertThatThrownBy(() -> StatementQuery.of(DAY, null, null, "up", null, null))
              .isInstanceOf(UnprocessableEntityException.class).hasMessage("order must be asc or desc");
        assertThatThrownBy(() -> StatementQuery.of(DAY, null, null, null, -1, null))
              .isInstanceOf(UnprocessableEntityException.class).hasMessage("page must be 0 or greater");
        assertThatThrownBy(() -> StatementQuery.of(DAY, null, null, null, null, 0))
              .isInstanceOf(UnprocessableEntityException.class).hasMessage("size must be between 1 and 100");
        assertThatThrownBy(() -> StatementQuery.of(DAY, null, null, null, null, 101))
              .isInstanceOf(UnprocessableEntityException.class).hasMessage("size must be between 1 and 100");
    }
}
