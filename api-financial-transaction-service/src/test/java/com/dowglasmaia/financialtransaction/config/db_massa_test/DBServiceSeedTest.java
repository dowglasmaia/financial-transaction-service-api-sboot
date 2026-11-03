package com.dowglasmaia.financialtransaction.config.db_massa_test;

import com.dowglasmaia.financialtransaction.entity.Account;
import com.dowglasmaia.financialtransaction.entity.Agency;
import com.dowglasmaia.financialtransaction.entity.User;
import com.dowglasmaia.financialtransaction.repository.AccountRepository;
import com.dowglasmaia.financialtransaction.repository.AgencyRepository;
import com.dowglasmaia.financialtransaction.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Local/test seed: the demo customer is linked to the owner subject; an optional second customer for a second login. */
@ExtendWith(MockitoExtension.class)
class DBServiceSeedTest {

    @Mock UserRepository users;
    @Mock AgencyRepository agencies;
    @Mock AccountRepository accounts;

    DBService seed;
    Agency agency = Agency.builder().id("agency-1").number("0211").build();
    User demoUser = User.builder().id("user-1").name("Demo User").build();
    Account demoAccount = Account.builder().id("acc-1").number("2022").user(demoUser).agency(agency).balance(new BigDecimal("5000")).build();

    @BeforeEach
    void setUp() {
        seed = new DBService(users, agencies, accounts);
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

        assertThat(demoUser.getExternalSubject()).isEqualTo("auth0|owner");
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
        User secondUser = User.builder().id("user-2").externalSubject("auth0|second").build();
        when(accounts.findByNumber("3033")).thenReturn(Optional.of(Account.builder().id("acc-2").number("3033").user(secondUser).build()));
        ReflectionTestUtils.setField(seed, "secondSubject", "auth0|second");

        seed.instanciateTestDatabase();

        verify(accounts, never()).save(any(Account.class));
    }
}
