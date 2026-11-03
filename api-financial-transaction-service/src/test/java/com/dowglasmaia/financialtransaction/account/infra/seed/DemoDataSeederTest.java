package com.dowglasmaia.financialtransaction.account.infra.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.account.domain.AccountRepository;
import com.dowglasmaia.financialtransaction.account.domain.Agency;
import com.dowglasmaia.financialtransaction.account.domain.AgencyRepository;
import com.dowglasmaia.financialtransaction.account.domain.User;
import com.dowglasmaia.financialtransaction.account.domain.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** Local/test seed: the demo customer is linked to the owner subject; an optional second customer for a second login. */
@ExtendWith(MockitoExtension.class)
class DemoDataSeederTest {

    @Mock UserRepository users;
    @Mock AgencyRepository agencies;
    @Mock AccountRepository accounts;

    DemoDataSeeder seed;
    Agency agency = Agency.restore("agency-1", "0211");
    User demoUser = User.restore("user-1", "Demo User", null, null, null, null, null);
    Account demoAccount = Account.restore("acc-1", agency, demoUser, "2022", new BigDecimal("5000"));

    @BeforeEach
    void setUp() {
        seed = new DemoDataSeeder(users, agencies, accounts);
        lenient().when(agencies.findByNumber("0211")).thenReturn(Optional.of(agency));
        lenient().when(accounts.findByNumber("2022")).thenReturn(Optional.of(demoAccount));
        lenient().when(accounts.findByNumber("3033")).thenReturn(Optional.empty());
        lenient().when(users.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        lenient().when(accounts.save(any(Account.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void withoutASecondSubjectOnlyTheDemoCustomerExists() {
        ReflectionTestUtils.setField(seed, "ownerSubject", "auth0|owner");

        seed.instanciateTestDatabase();

        ArgumentCaptor<User> linked = ArgumentCaptor.forClass(User.class);
        verify(users).save(linked.capture());
        assertThat(linked.getValue().getId()).isEqualTo("user-1");
        assertThat(linked.getValue().getExternalSubject()).isEqualTo("auth0|owner");
        verify(accounts, never()).save(any(Account.class));
    }

    @Test
    void aSecondSubjectCreatesALinkedSecondCustomerWithItsOwnAccount() {
        ReflectionTestUtils.setField(seed, "ownerSubject", "auth0|owner");
        ReflectionTestUtils.setField(seed, "secondSubject", "auth0|second");

        seed.instanciateTestDatabase();

        ArgumentCaptor<Account> saved = ArgumentCaptor.forClass(Account.class);
        verify(accounts).save(saved.capture());
        Account second = saved.getValue();
        assertThat(second.getNumber()).isEqualTo("3033");
        assertThat(second.getBalance()).isEqualByComparingTo("1000");
        assertThat(second.getUser().getExternalSubject()).isEqualTo("auth0|second");
        assertThat(second.getUser().getId()).isNotEqualTo(demoUser.getId());
    }

    @Test
    void reseedingDoesNotDuplicateAnExistingSecondCustomer() {
        User secondUser = User.restore("user-2", null, null, null, null, null, "auth0|second");
        when(accounts.findByNumber("3033")).thenReturn(Optional.of(Account.restore("acc-2", null, secondUser, "3033", null)));
        ReflectionTestUtils.setField(seed, "secondSubject", "auth0|second");

        seed.instanciateTestDatabase();

        verify(accounts, never()).save(any(Account.class));
        verify(users, never()).save(any(User.class));
    }

    @Test
    void anExistingSecondCustomerLinkedToAnotherSubjectIsRelinkedNotDuplicated() {
        User secondUser = User.restore("user-2", null, null, null, null, null, "auth0|old-subject");
        when(accounts.findByNumber("3033")).thenReturn(Optional.of(Account.restore("acc-2", null, secondUser, "3033", null)));
        ReflectionTestUtils.setField(seed, "secondSubject", "auth0|second");

        seed.instanciateTestDatabase();

        ArgumentCaptor<User> relinked = ArgumentCaptor.forClass(User.class);
        verify(users).save(relinked.capture());
        assertThat(relinked.getValue().getId()).isEqualTo("user-2");
        assertThat(relinked.getValue().getExternalSubject()).isEqualTo("auth0|second");
        verify(accounts, never()).save(any(Account.class));
    }
}
