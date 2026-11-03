package com.dowglasmaia.financialtransaction.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.dowglasmaia.financialtransaction.account.application.port.CallerIdentity;
import com.dowglasmaia.financialtransaction.account.application.port.UserRepository;
import com.dowglasmaia.financialtransaction.account.domain.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckCallerLinkedUseCaseTest {

    @Mock UserRepository users;
    @Mock CallerIdentity caller;

    @Test
    void tellsWhetherTheSubjectHasAUser() {
        when(caller.subject()).thenReturn(Optional.of("auth0|abc"), Optional.of("auth0|stranger"));
        when(users.findByExternalSubject("auth0|abc")).thenReturn(Optional.of(User.restore("user-1", "Demo", null, null, null, null, "auth0|abc")));
        when(users.findByExternalSubject("auth0|stranger")).thenReturn(Optional.empty());

        CheckCallerLinkedUseCase useCase = new CheckCallerLinkedUseCase(users, caller);

        assertThat(useCase.execute()).isTrue();
        assertThat(useCase.execute()).isFalse();
    }
}
