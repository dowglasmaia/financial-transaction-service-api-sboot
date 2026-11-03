package com.dowglasmaia.financialtransaction.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dowglasmaia.financialtransaction.account.application.port.AccountRepository;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.account.domain.Agency;
import com.dowglasmaia.financialtransaction.account.domain.User;
import com.dowglasmaia.financialtransaction.shared.error.ObjectNotFoundExeption;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DebitAccountByNumberUseCaseTest {

    @Mock AccountRepository accounts;

    private final Account account = Account.restore("acc-1", Agency.restore("agency-1", "0211"),
          User.restore("user-1", "Demo", "11144477735", "d@example.test", 11, 999998888, null), "2022", new BigDecimal("5000.00"));

    private DebitAccountByNumberUseCase useCase() {
        return new DebitAccountByNumberUseCase(accounts, new DebitAccountUseCase(accounts));
    }

    @Test
    void debitsTheAccountFoundByNumber() {
        when(accounts.findByNumber("2022")).thenReturn(Optional.of(account));
        when(accounts.tryDebit("acc-1", BigDecimal.TEN)).thenReturn(true);

        useCase().execute("2022", BigDecimal.TEN);

        verify(accounts).tryDebit("acc-1", BigDecimal.TEN);
    }

    @Test
    void anUnknownAccountNumberIsNotFound() {
        when(accounts.findByNumber("9999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute("9999", BigDecimal.TEN))
              .isInstanceOf(ObjectNotFoundExeption.class).hasMessage("Account not found.");
    }
}
