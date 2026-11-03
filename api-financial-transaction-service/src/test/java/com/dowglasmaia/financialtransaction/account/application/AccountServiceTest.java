package com.dowglasmaia.financialtransaction.account.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.account.application.port.AccountRepository;
import com.dowglasmaia.financialtransaction.account.domain.Agency;
import com.dowglasmaia.financialtransaction.account.application.port.AgencyRepository;
import com.dowglasmaia.financialtransaction.account.application.port.CallerIdentity;
import com.dowglasmaia.financialtransaction.account.domain.User;
import com.dowglasmaia.financialtransaction.account.application.port.UserRepository;
import com.dowglasmaia.financialtransaction.exeptions.ForbiddenExeption;
import com.dowglasmaia.financialtransaction.exeptions.ObjectNotFoundExeption;
import com.dowglasmaia.financialtransaction.exeptions.UnprocessableEntityExeption;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final BigDecimal TEN = BigDecimal.TEN;

    @Mock AccountRepository accounts;
    @Mock AgencyRepository agencies;
    @Mock UserRepository users;
    @Mock CallerIdentity caller;

    private AccountService service() {
        return new AccountService(accounts, agencies, users, caller);
    }

    private final Agency agency = Agency.restore("agency-1", "0211");
    private final User holder = User.restore("user-1", "Demo", "11144477735", "d@example.test", 11, 999998888, "auth0|abc");
    private final Account account = Account.restore("acc-1", agency, holder, "2022", new BigDecimal("5000.00"));

    @Test
    void debitByNumberDebitsTheAccountFound() {
        when(accounts.findByNumber("2022")).thenReturn(Optional.of(account));
        when(accounts.tryDebit("acc-1", TEN)).thenReturn(true);

        service().debit("2022", TEN);

        verify(accounts).tryDebit("acc-1", TEN);
    }

    @Test
    void debitOfAnUnknownAccountNumberIsNotFound() {
        when(accounts.findByNumber("9999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().debit("9999", TEN))
              .isInstanceOf(ObjectNotFoundExeption.class).hasMessage("Account not found.");
    }

    @Test
    void aDebitTheBalanceDoesNotCoverIsUnprocessableWithTheKnownMessage() {
        when(accounts.tryDebit("acc-1", TEN)).thenReturn(false);

        assertThatThrownBy(() -> service().debitById("acc-1", TEN))
              .isInstanceOf(UnprocessableEntityExeption.class)
              .hasMessage("Operation not performed, there is not enough balance in the account.");
    }

    @Test
    void creditGoesThroughThePort() {
        when(accounts.credit("acc-1", TEN)).thenReturn(true);

        service().credit("acc-1", TEN);

        verify(accounts).credit("acc-1", TEN);
    }

    @Test
    void theCurrentAccountIsTheOneOfTheUserLinkedToTheTokenSubject() {
        when(caller.subject()).thenReturn(Optional.of("auth0|abc"));
        when(users.findByExternalSubject("auth0|abc")).thenReturn(Optional.of(holder));
        when(accounts.findFirstByUserId("user-1")).thenReturn(Optional.of(account));

        assertThat(service().currentAccount()).isSameAs(account);
    }

    @Test
    void withoutATokenTheCallerIsForbidden() {
        when(caller.subject()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().currentAccount())
              .isInstanceOf(ForbiddenExeption.class).hasMessage("Access denied.");
    }

    @Test
    void aSubjectNotLinkedToAUserIsForbiddenWithTheKnownMessage() {
        when(caller.subject()).thenReturn(Optional.of("auth0|stranger"));
        when(users.findByExternalSubject("auth0|stranger")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().currentAccount())
              .isInstanceOf(ForbiddenExeption.class).hasMessage("Access denied: this identity is not linked to a customer.");
    }

    @Test
    void aLinkedUserWithoutAnAccountIsForbiddenToo() {
        when(caller.subject()).thenReturn(Optional.of("auth0|abc"));
        when(users.findByExternalSubject("auth0|abc")).thenReturn(Optional.of(holder));
        when(accounts.findFirstByUserId("user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().currentAccount())
              .isInstanceOf(ForbiddenExeption.class).hasMessage("Access denied: this identity is not linked to a customer.");
    }

    @Test
    void isLinkedTellsWhetherTheSubjectHasAUser() {
        when(caller.subject()).thenReturn(Optional.of("auth0|abc"), Optional.of("auth0|stranger"));
        when(users.findByExternalSubject("auth0|abc")).thenReturn(Optional.of(holder));
        when(users.findByExternalSubject("auth0|stranger")).thenReturn(Optional.empty());

        assertThat(service().isLinked()).isTrue();
        assertThat(service().isLinked()).isFalse();
    }

    @Test
    void theCurrentAgencyIsTheDemoAgency0211() {
        when(agencies.findByNumber("0211")).thenReturn(Optional.of(agency));

        assertThat(service().currentAgencyId()).isEqualTo("agency-1");
    }

    @Test
    void aMissingCurrentAgencyIsNotFound() {
        when(agencies.findByNumber("0211")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().currentAgencyId())
              .isInstanceOf(ObjectNotFoundExeption.class).hasMessage("Agency not found.");
    }
}
