package com.dowglasmaia.financialtransaction.account.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.dowglasmaia.financialtransaction.account.application.usecase.CheckCallerLinkedUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

/** The decision is unchanged (role customer AND linked); the reason of a refusal is now logged so a 403 can be explained. */
@ExtendWith(MockitoExtension.class)
class CustomerLinkedAuthorizationTest {

    @Mock CheckCallerLinkedUseCase checkCallerLinked;

    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(CustomerLinkedAuthorization.class);

    @BeforeEach
    void captureLogs() {
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void releaseLogs() {
        logger.detachAppender(logs);
    }

    private boolean granted(Authentication caller) {
        return new CustomerLinkedAuthorization(checkCallerLinked).check(() -> caller, null).isGranted();
    }

    private String logged() {
        return logs.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("", (a, b) -> a + "\n" + b);
    }

    @Test
    void aCustomerLinkedToAUserIsGrantedAndNothingIsLogged() {
        when(checkCallerLinked.execute()).thenReturn(true);

        assertThat(granted(new TestingAuthenticationToken("u", "p", "ROLE_CUSTOMER"))).isTrue();
        assertThat(logs.list).isEmpty();
    }

    @Test
    void aTokenWithoutTheCustomerRoleIsRefusedWithoutLookingUpTheUserAndTheRolesAreLogged() {
        assertThat(granted(new TestingAuthenticationToken("u", "p", "ROLE_ADMIN"))).isFalse();

        verifyNoInteractions(checkCallerLinked);
        assertThat(logged()).contains("no ROLE_CUSTOMER").contains("ROLE_ADMIN");
    }

    @Test
    void aTokenWithNoRolesAtAllSaysSo() {
        assertThat(granted(new TestingAuthenticationToken("u", "p"))).isFalse();

        assertThat(logged()).contains("no ROLE_CUSTOMER").contains("[]");
    }

    @Test
    void aCustomerWhoseSubjectIsNotLinkedIsRefusedAndTheReasonIsLogged() {
        when(checkCallerLinked.execute()).thenReturn(false);

        assertThat(granted(new TestingAuthenticationToken("u", "p", "ROLE_CUSTOMER"))).isFalse();
        assertThat(logged()).contains("not linked to a customer");
    }

    @Test
    void anAnonymousCallerIsRefusedQuietly() { // the 401 path: nothing to explain, no noise
        Authentication anonymous = new AnonymousAuthenticationToken("key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

        assertThat(granted(anonymous)).isFalse();
        assertThat(logs.list).isEmpty();
    }
}
