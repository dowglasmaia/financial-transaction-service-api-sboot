package com.dowglasmaia.financialtransaction.account.application.usecase;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dowglasmaia.financialtransaction.account.application.port.AccountRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreditAccountUseCaseTest {

    @Mock AccountRepository accounts;

    @Test
    void creditsThroughThePort() {
        when(accounts.credit("acc-1", BigDecimal.TEN)).thenReturn(true);

        new CreditAccountUseCase(accounts).execute("acc-1", BigDecimal.TEN);

        verify(accounts).credit("acc-1", BigDecimal.TEN);
    }
}
