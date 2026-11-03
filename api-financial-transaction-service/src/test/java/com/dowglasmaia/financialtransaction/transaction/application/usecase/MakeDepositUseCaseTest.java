package com.dowglasmaia.financialtransaction.transaction.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dowglasmaia.financialtransaction.account.application.usecase.CreditAccountUseCase;
import com.dowglasmaia.financialtransaction.account.application.usecase.FindAccountByAgencyAndNumberUseCase;
import com.dowglasmaia.financialtransaction.account.application.usecase.GetCurrentAgencyUseCase;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import com.dowglasmaia.financialtransaction.exeptions.ObjectNotFoundExeption;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MakeDepositUseCaseTest {

    @Mock GetCurrentAgencyUseCase getCurrentAgency;
    @Mock FindAccountByAgencyAndNumberUseCase findAccount;
    @Mock CreditAccountUseCase creditAccount;
    @Mock SaveTransactionUseCase saveTransaction;

    private MakeDepositUseCase useCase() {
        return new MakeDepositUseCase(getCurrentAgency, findAccount, creditAccount, saveTransaction);
    }

    @Test
    void creditsTheAccountAndWritesTheLedgerRow() {
        when(getCurrentAgency.execute()).thenReturn("agency-id");
        when(findAccount.execute("agency-id", "2022")).thenReturn(Optional.of(Account.restore("acc-1", null, null, "2022", BigDecimal.ONE)));

        useCase().execute("0211", "2022", new BigDecimal("80.00"));

        verify(creditAccount).execute("acc-1", new BigDecimal("80.00"));
        verify(saveTransaction).execute(TransactionType.DEPOSIT, "acc-1", new BigDecimal("80.00"), "agency-id");
    }

    @Test
    void anUnknownAccountIsNotFoundAndNothingIsCreditedOrWritten() {
        when(getCurrentAgency.execute()).thenReturn("agency-id");
        when(findAccount.execute("agency-id", "9999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute("0211", "9999", BigDecimal.TEN))
              .isInstanceOf(ObjectNotFoundExeption.class).hasMessage("Account not found.");

        verifyNoInteractions(creditAccount, saveTransaction);
    }
}
