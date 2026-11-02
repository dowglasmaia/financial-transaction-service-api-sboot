package com.dowglasmaia.financialtransaction.services.impl;

import com.dowglasmaia.financialtransaction.entity.Account;
import com.dowglasmaia.financialtransaction.entity.TransactionTypeEnum;
import com.dowglasmaia.financialtransaction.services.TransactionService;
import com.dowglasmaia.financialtransaction.services.client.account.AddAmountService;
import com.dowglasmaia.financialtransaction.services.client.account.FindAccountByNumberAndUserIdService;
import com.dowglasmaia.financialtransaction.services.client.agency.AgencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static com.dowglasmaia.financialtransaction.mock.MockBuild.userAccountBuild;
import static com.dowglasmaia.financialtransaction.mock.MockBuild.withdrawRequestBuild;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/** Unit test of the card / non-card branching (spec BE-01: replaces the previous vacuous assertions). */
@ExtendWith(MockitoExtension.class)
class AtmWithdrawalServiceImplTest {

    @Mock AgencyService agencyService;
    @Mock AddAmountService addAmountService;
    @Mock TransactionService transactionService;
    @Mock FindAccountByNumberAndUserIdService findAccount;

    AtmWithdrawalServiceImpl service;
    Account actingAccount;

    @BeforeEach
    void setUp() {
        service = new AtmWithdrawalServiceImpl(agencyService, addAmountService, transactionService, findAccount);
        actingAccount = userAccountBuild();
        actingAccount.setNumber("2022");
        actingAccount.setId("acting-id");
        when(agencyService.getCurrentAgency()).thenReturn("agency-id");
        when(findAccount.findByNumberAndUserId()).thenReturn(actingAccount);
    }

    @Test
    void cardBasedWithdrawalDebitsTheActingAccount() {
        var request = withdrawRequestBuild(null, null, new BigDecimal("150.00"));

        service.makeAtmWithdrawal(request);

        verify(addAmountService).debitById("acting-id", new BigDecimal("150.00"));
        verify(transactionService).save(TransactionTypeEnum.WITHDRAW, "acting-id", new BigDecimal("150.00"), "agency-id");
        verifyNoMoreInteractions(addAmountService);
    }

    @Test
    void nonCardWithdrawalDebitsTheAccountNumberFromTheRequestButLogsTheActingAccount() { // characterizes F-05
        var request = withdrawRequestBuild("9999", "0211", new BigDecimal("80.00"));

        service.makeAtmWithdrawal(request);

        verify(addAmountService).debit(eq("9999"), eq(new BigDecimal("80.00")));
        verify(transactionService).save(eq(TransactionTypeEnum.WITHDRAW), eq("acting-id"), eq(new BigDecimal("80.00")), eq("agency-id"));
    }
}
