package com.dowglasmaia.financialtransaction.shared.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** BE-08: money is positive and has at most two decimal places. More places are REJECTED, never rounded (owner decision). */
class MoneyTest {

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "10", "10.5", "10.50", "10.500", "1E+3", "999999999.99"})
    void aPositiveAmountWithAtMostTwoDecimalPlacesIsAccepted(String value) {
        assertThat(Money.requirePositive(new BigDecimal(value), "m").value()).isEqualByComparingTo(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-0.01", "-5"})
    void zeroOrNegativeIsRefusedWithTheCallersMessage(String value) {
        assertThatThrownBy(() -> Money.requirePositive(new BigDecimal(value), "must be positive"))
              .isInstanceOf(IllegalArgumentException.class).hasMessage("must be positive");
    }

    @Test
    void nullIsRefusedWithTheCallersMessage() {
        assertThatThrownBy(() -> Money.requirePositive(null, "must be positive"))
              .isInstanceOf(IllegalArgumentException.class).hasMessage("must be positive");
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.005", "0.001", "1.234", "99.999"})
    void moreThanTwoDecimalPlacesIsRefusedNotRounded(String value) {
        assertThatThrownBy(() -> Money.requirePositive(new BigDecimal(value), "m"))
              .isInstanceOf(IllegalArgumentException.class).hasMessage("Amount must have at most 2 decimal places");
    }

    @Test
    void aBalanceMayBeZeroButNeverNegative() {
        assertThat(Money.balance(BigDecimal.ZERO).value()).isEqualByComparingTo("0");
        assertThat(Money.balance(new BigDecimal("5000.00")).value()).isEqualByComparingTo("5000");
        assertThatThrownBy(() -> Money.balance(new BigDecimal("-0.01"))).isInstanceOf(IllegalArgumentException.class).hasMessage("Balance cannot be negative");
        assertThatThrownBy(() -> Money.balance(null)).isInstanceOf(IllegalArgumentException.class).hasMessage("Balance is required");
    }
}
