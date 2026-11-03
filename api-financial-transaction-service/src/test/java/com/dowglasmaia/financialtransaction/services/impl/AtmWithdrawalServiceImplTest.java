package com.dowglasmaia.financialtransaction.services.impl;

import com.dowglasmaia.financialtransaction.account.application.AccountService;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.entity.TransactionTypeEnum;
import com.dowglasmaia.financialtransaction.services.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static com.dowglasmaia.financialtransaction.mock.MockBuild.withdrawRequestBuild;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/** Unit test of the card / non-card branching (spec BE-01: replaces the previous vacuous assertions). */
@ExtendWith(MockitoExtension.class)
class AtmWithdrawalServiceImplTest {

    @Mock AccountService accountService;
    @Mock TransactionService transactionService;

    AtmWithdrawalServiceImpl service;
    Account actingAccount;

    @BeforeEach
    void setUp() {
        service = new AtmWithdrawalServiceImpl(accountService, transactionService);
        actingAccount = Account.restore("acting-id", null, null, "2022", null);
        when(accountService.currentAgencyId()).thenReturn("agency-id");
        when(accountService.currentAccount()).thenReturn(actingAccount);
    }

    @Test
    void cardBasedWithdrawalDebitsTheActingAccount() {
        var request = withdrawRequestBuild(null, null, new BigDecimal("150.00"));

        service.makeAtmWithdrawal(request);

        verify(accountService).debitById("acting-id", new BigDecimal("150.00"));
        verify(transactionService).save(TransactionTypeEnum.WITHDRAW, "acting-id", new BigDecimal("150.00"), "agency-id");
        verify(accountService).currentAgencyId();
        verify(accountService).currentAccount();
        verifyNoMoreInteractions(accountService);
    }

    @Test
    void nonCardWithdrawalOnTheCallersOwnAccountDebitsItAndLogsIt() {
        var request = withdrawRequestBuild("2022", "0211", new BigDecimal("80.00"));

        service.makeAtmWithdrawal(request);

        verify(accountService).debitById("acting-id", new BigDecimal("80.00"));
        verify(transactionService).save(eq(TransactionTypeEnum.WITHDRAW), eq("acting-id"), eq(new BigDecimal("80.00")), eq("agency-id"));
    }

    @Test
    void nonCardWithdrawalOnAnotherAccountNumberIsNotFoundAndDebitsNothing() { // F-05 fixed by BE-04
        var request = withdrawRequestBuild("9999", "0211", new BigDecimal("80.00"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.makeAtmWithdrawal(request))
              .isInstanceOf(com.dowglasmaia.financialtransaction.exeptions.ObjectNotFoundExeption.class)
              .hasMessage("Account not found.");

        verify(accountService).currentAgencyId();
        verify(accountService).currentAccount();
        verifyNoMoreInteractions(accountService, transactionService);
    }
}
