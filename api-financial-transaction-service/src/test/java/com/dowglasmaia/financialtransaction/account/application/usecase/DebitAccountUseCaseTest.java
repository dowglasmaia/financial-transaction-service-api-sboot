package com.dowglasmaia.financialtransaction.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dowglasmaia.financialtransaction.account.application.port.AccountRepository;
import com.dowglasmaia.financialtransaction.exeptions.UnprocessableEntityExeption;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DebitAccountUseCaseTest {

    @Mock AccountRepository accounts;

    @Test
    void debitsThroughTheAtomicPortMethod() {
        when(accounts.tryDebit("acc-1", BigDecimal.TEN)).thenReturn(true);

        new DebitAccountUseCase(accounts).execute("acc-1", BigDecimal.TEN);

        verify(accounts).tryDebit("acc-1", BigDecimal.TEN);
    }

    @Test
    void aDebitTheBalanceDoesNotCoverIsUnprocessableWithTheKnownMessage() {
        when(accounts.tryDebit("acc-1", BigDecimal.TEN)).thenReturn(false);

        assertThatThrownBy(() -> new DebitAccountUseCase(accounts).execute("acc-1", BigDecimal.TEN))
              .isInstanceOf(UnprocessableEntityExeption.class)
              .hasMessage("Operation not performed, there is not enough balance in the account.");
    }
}
