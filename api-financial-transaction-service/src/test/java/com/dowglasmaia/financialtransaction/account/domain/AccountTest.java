package com.dowglasmaia.financialtransaction.account.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AccountTest {

    private final Agency agency = Agency.restore("agency-1", "0211");
    private final User user = User.restore("user-1", "Demo User", "11144477735", "demo@example.test", 11, 999998888, null);

    @Test
    void restoreKeepsEveryField() {
        Account account = Account.restore("acc-1", agency, user, "2022", new BigDecimal("5000.00"));

        assertThat(account.getId()).isEqualTo("acc-1");
        assertThat(account.getAgency()).isSameAs(agency);
        assertThat(account.getUser()).isSameAs(user);
        assertThat(account.getNumber()).isEqualTo("2022");
        assertThat(account.getBalance()).isEqualByComparingTo("5000.00");
    }

    @Test
    void createHasNoIdYet() {
        Account account = Account.create(agency, user, "3033", new BigDecimal("1000.0"));

        assertThat(account.getId()).isNull();
        assertThat(account.getNumber()).isEqualTo("3033");
    }

    @Test
    void createRequiresAgencyUserAndNumber() {
        assertThatThrownBy(() -> Account.create(null, user, "1", BigDecimal.ONE)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Account.create(agency, null, "1", BigDecimal.ONE)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Account.create(agency, user, " ", BigDecimal.ONE)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRefusesANegativeOrMissingBalance() { // BE-08 invariant
        assertThatThrownBy(() -> Account.create(agency, user, "3033", new BigDecimal("-0.01"))).isInstanceOf(IllegalArgumentException.class).hasMessage("Balance cannot be negative");
        assertThatThrownBy(() -> Account.create(agency, user, "3033", null)).isInstanceOf(IllegalArgumentException.class).hasMessage("Balance is required");
        assertThat(Account.create(agency, user, "3033", BigDecimal.ZERO).getBalance()).isEqualByComparingTo("0");
    }
}
