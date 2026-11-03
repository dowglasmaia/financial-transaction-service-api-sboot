package com.dowglasmaia.financialtransaction.account.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.account.application.port.AccountRepository;
import com.dowglasmaia.financialtransaction.account.application.port.AgencyRepository;
import com.dowglasmaia.financialtransaction.account.domain.User;
import com.dowglasmaia.financialtransaction.account.application.port.UserRepository;
import com.dowglasmaia.financialtransaction.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Round trips of the account adapters against the real PostgreSQL schema (ports in, domain objects out). */
@Transactional // the modifying queries need a transaction; in production AccountService provides it
class AccountRepositoryAdapterTest extends AbstractIntegrationTest {

    @Autowired AccountRepository accounts;
    @Autowired AgencyRepository agencies;
    @Autowired UserRepository users;

    @Test
    void findsTheSeedAccountAsADomainObjectWithItsAgencyAndHolder() {
        Account account = accounts.findByNumber(SEED_ACCOUNT).orElseThrow();

        assertThat(account.getId()).isNotBlank();
        assertThat(account.getAgency().getNumber()).isEqualTo(SEED_AGENCY);
        assertThat(account.getUser().getName()).isEqualTo("Demo User");
        assertThat(account.getBalance()).isEqualByComparingTo(SEED_BALANCE);
    }

    @Test
    void findsByAgencyAndNumber() {
        String agencyId = agencies.findByNumber(SEED_AGENCY).orElseThrow().getId();

        assertThat(accounts.findByAgencyAndNumber(agencyId, SEED_ACCOUNT)).isPresent();
        assertThat(accounts.findByAgencyAndNumber(agencyId, "0000")).isEmpty();
    }

    @Test
    void debitsOnlyWhenTheBalanceCoversTheAmountAndAnExactBalanceIsEnough() {
        String id = accounts.findByNumber(SEED_ACCOUNT).orElseThrow().getId();

        assertThat(accounts.tryDebit(id, new BigDecimal("5000.01"))).isFalse();
        assertThat(balanceOf(SEED_ACCOUNT)).isEqualByComparingTo(SEED_BALANCE);

        assertThat(accounts.tryDebit(id, new BigDecimal("1200.50"))).isTrue();
        assertThat(balanceOf(SEED_ACCOUNT)).isEqualByComparingTo("3799.50");

        assertThat(accounts.tryDebit(id, new BigDecimal("3799.50"))).isTrue();
        assertThat(balanceOf(SEED_ACCOUNT)).isEqualByComparingTo("0.00");
    }

    @Test
    void creditsAnExistingAccountAndReportsAnUnknownOne() {
        String id = accounts.findByNumber(SEED_ACCOUNT).orElseThrow().getId();

        assertThat(accounts.credit(id, new BigDecimal("10.25"))).isTrue();
        assertThat(balanceOf(SEED_ACCOUNT)).isEqualByComparingTo("5010.25");
        assertThat(accounts.credit("no-such-account", BigDecimal.TEN)).isFalse();
    }

    @Test
    void savesAHolderAndAnAccountAndFindsTheAccountByItsHolder() {
        User holder = users.save(User.create("Round Trip", "39053344705", "round.trip@example.test", 11, 977776666)
              .linkedTo("auth0|round-trip"));
        Account saved = accounts.save(Account.create(agencies.findByNumber(SEED_AGENCY).orElseThrow(), holder, "7777", new BigDecimal("42.00")));
        try {
            assertThat(saved.getId()).isNotBlank();
            assertThat(users.findByExternalSubject("auth0|round-trip")).get().extracting(User::getId).isEqualTo(holder.getId());
            assertThat(accounts.findFirstByUserId(holder.getId())).get().extracting(Account::getNumber).isEqualTo("7777");
        } finally {
            accountRepository.deleteById(saved.getId());
            userRepository.deleteById(holder.getId());
        }
    }

    @Test
    void anUnknownSubjectHasNoUser() {
        assertThat(users.findByExternalSubject("auth0|nobody")).isEmpty();
    }
}
