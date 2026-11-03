package com.dowglasmaia.financialtransaction.transaction.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MobileRechargeTest {

    @Test
    void thePhoneIsTheAreaCodeFollowedByTheNumber() {
        MobileRecharge recharge = new MobileRecharge(11, 999998888, new BigDecimal("20.00"), "VIVO");

        assertThat(recharge.fullPhoneNumber()).isEqualTo("11999998888");
        assertThat(recharge.hasValidPhone()).isTrue();
    }

    @Test
    void aShortOrMalformedPhoneIsNotValid() {
        assertThat(new MobileRecharge(11, 99998888, BigDecimal.TEN, "TIM").hasValidPhone()).isFalse();
        assertThat(new MobileRecharge(1, 999998888, BigDecimal.TEN, "TIM").hasValidPhone()).isFalse();
    }
}
