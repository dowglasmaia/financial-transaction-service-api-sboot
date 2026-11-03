package com.dowglasmaia.financialtransaction.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.dowglasmaia.financialtransaction.account.application.port.AccountRepository;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindAccountByAgencyAndNumberUseCaseTest {

    @Mock AccountRepository accounts;

    @Test
    void returnsWhatThePortFinds() {
        Account account = Account.restore("acc-1", null, null, "2022", BigDecimal.ONE);
        when(accounts.findByAgencyAndNumber("agency-1", "2022")).thenReturn(Optional.of(account));
        when(accounts.findByAgencyAndNumber("agency-1", "0000")).thenReturn(Optional.empty());

        FindAccountByAgencyAndNumberUseCase useCase = new FindAccountByAgencyAndNumberUseCase(accounts);

        assertThat(useCase.execute("agency-1", "2022")).containsSame(account);
        assertThat(useCase.execute("agency-1", "0000")).isEmpty();
    }
}
