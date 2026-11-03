package com.dowglasmaia.financialtransaction.transaction.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dowglasmaia.financialtransaction.account.application.usecase.DebitAccountUseCase;
import com.dowglasmaia.financialtransaction.account.application.usecase.GetCurrentAccountUseCase;
import com.dowglasmaia.financialtransaction.account.application.usecase.GetCurrentAgencyUseCase;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import com.dowglasmaia.financialtransaction.exeptions.ObjectNotFoundExeption;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit test of the card / non-card branching (spec BE-01), moved from AtmWithdrawalServiceImplTest. */
@ExtendWith(MockitoExtension.class)
class MakeAtmWithdrawalUseCaseTest {

    @Mock GetCurrentAgencyUseCase getCurrentAgency;
    @Mock GetCurrentAccountUseCase getCurrentAccount;
    @Mock DebitAccountUseCase debitAccount;
    @Mock SaveTransactionUseCase saveTransaction;

    MakeAtmWithdrawalUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new MakeAtmWithdrawalUseCase(getCurrentAgency, getCurrentAccount, debitAccount, saveTransaction);
        when(getCurrentAgency.execute()).thenReturn("agency-id");
        when(getCurrentAccount.execute()).thenReturn(Account.restore("acting-id", null, null, "2022", null));
    }

    @Test
    void cardBasedWithdrawalDebitsTheActingAccount() {
        useCase.execute(null, null, new BigDecimal("150.00"));

        verify(debitAccount).execute("acting-id", new BigDecimal("150.00"));
        verify(saveTransaction).execute(TransactionType.WITHDRAW, "acting-id", new BigDecimal("150.00"), "agency-id");
    }

    @Test
    void nonCardWithdrawalOnTheCallersOwnAccountDebitsItAndLogsIt() {
        useCase.execute("0211", "2022", new BigDecimal("80.00"));

        verify(debitAccount).execute("acting-id", new BigDecimal("80.00"));
        verify(saveTransaction).execute(TransactionType.WITHDRAW, "acting-id", new BigDecimal("80.00"), "agency-id");
    }

    @Test
    void nonCardWithdrawalOnAnotherAccountNumberIsNotFoundAndDebitsNothing() { // F-05 fixed by BE-04
        assertThatThrownBy(() -> useCase.execute("0211", "9999", new BigDecimal("80.00")))
              .isInstanceOf(ObjectNotFoundExeption.class).hasMessage("Account not found.");

        verifyNoInteractions(debitAccount, saveTransaction);
    }
}
