package com.dowglasmaia.financialtransaction.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.dowglasmaia.financialtransaction.account.application.port.AccountRepository;
import com.dowglasmaia.financialtransaction.account.application.port.CallerIdentity;
import com.dowglasmaia.financialtransaction.account.application.port.UserRepository;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.account.domain.Agency;
import com.dowglasmaia.financialtransaction.account.domain.User;
import com.dowglasmaia.financialtransaction.shared.error.ForbiddenException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetCurrentAccountUseCaseTest {

    private static final String NOT_LINKED = "Access denied: this identity is not linked to a customer.";

    @Mock AccountRepository accounts;
    @Mock UserRepository users;
    @Mock CallerIdentity caller;

    private final User holder = User.restore("user-1", "Demo", "11144477735", "d@example.test", 11, 999998888, "auth0|abc");
    private final Account account = Account.restore("acc-1", Agency.restore("agency-1", "0211"), holder, "2022", new BigDecimal("5000.00"));

    private GetCurrentAccountUseCase useCase() {
        return new GetCurrentAccountUseCase(accounts, users, caller);
    }

    @Test
    void theCurrentAccountIsTheOneOfTheUserLinkedToTheTokenSubject() {
        when(caller.subject()).thenReturn(Optional.of("auth0|abc"));
        when(users.findByExternalSubject("auth0|abc")).thenReturn(Optional.of(holder));
        when(accounts.findFirstByUserId("user-1")).thenReturn(Optional.of(account));

        assertThat(useCase().execute()).isSameAs(account);
    }

    @Test
    void withoutATokenTheCallerIsForbidden() {
        when(caller.subject()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute()).isInstanceOf(ForbiddenException.class).hasMessage("Access denied.");
    }

    @Test
    void aSubjectNotLinkedToAUserIsForbiddenWithTheKnownMessage() {
        when(caller.subject()).thenReturn(Optional.of("auth0|stranger"));
        when(users.findByExternalSubject("auth0|stranger")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute()).isInstanceOf(ForbiddenException.class).hasMessage(NOT_LINKED);
    }

    @Test
    void aLinkedUserWithoutAnAccountIsForbiddenToo() {
        when(caller.subject()).thenReturn(Optional.of("auth0|abc"));
        when(users.findByExternalSubject("auth0|abc")).thenReturn(Optional.of(holder));
        when(accounts.findFirstByUserId("user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute()).isInstanceOf(ForbiddenException.class).hasMessage(NOT_LINKED);
    }
}
