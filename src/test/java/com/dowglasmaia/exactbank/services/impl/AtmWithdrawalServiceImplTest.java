package com.dowglasmaia.exactbank.services.impl;


import com.dowglasmaia.exactbank.entity.Account;
import com.dowglasmaia.exactbank.repository.AccountRepository;
import com.dowglasmaia.exactbank.services.TransactionService;
import com.dowglasmaia.exactbank.services.client.account.AddAmountService;
import com.dowglasmaia.exactbank.services.client.account.FindAccountByNumberAndUserIdService;
import com.dowglasmaia.exactbank.services.client.agency.AgencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static com.dowglasmaia.exactbank.mock.MockBuild.userAccountBuild;
import static com.dowglasmaia.exactbank.mock.MockBuild.withdrawRequestBuild;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;


@ActiveProfiles("test")
@ExtendWith({SpringExtension.class, MockitoExtension.class})
public class AtmWithdrawalServiceImplTest {

    private AtmWithdrawalServiceImpl service;

    @MockBean
    AccountRepository accountRepository;

    @MockBean
    private AgencyService agencyService;
    @MockBean
    private AddAmountService addAmountService;
    @MockBean
    private TransactionService transactionService;
    @MockBean
    private FindAccountByNumberAndUserIdService findAccountByNumberAndUserIdService;

    @BeforeEach
    public void setUp(){
        this.service = new AtmWithdrawalServiceImpl(
              agencyService,
              addAmountService,
              transactionService,
              findAccountByNumberAndUserIdService);

        var agencyId = String.valueOf(UUID.randomUUID());

        when(agencyService.getCurrentAgency()).thenReturn(agencyId);
        when(findAccountByNumberAndUserIdService.findByNumberAndUserId()).thenReturn(userAccountBuild());
        doNothing().when(addAmountService).addAmountToAccount(anyString(), any());
        doNothing().when(transactionService).save(any(), any(), any(), any());
    }

    @Test
    public void souldMakeAtmWithdrawalNotBasedCardSuccessfully(){
        var request = withdrawRequestBuild("21024", "1000", BigDecimal.valueOf(15000.00));

        service.makeAtmWithdrawal(request);

        assertThatNoException();

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    public void souldMakeAtmWithdrawalBasedCardSuccessfully(){
        var request = withdrawRequestBuild(null, null, BigDecimal.valueOf(15000.00));

        service.makeAtmWithdrawal(request);

        assertThatNoException();

        verify(accountRepository, never()).save(any(Account.class));
    }


}
