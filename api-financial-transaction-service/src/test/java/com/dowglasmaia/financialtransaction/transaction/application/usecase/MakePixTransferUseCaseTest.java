package com.dowglasmaia.financialtransaction.transaction.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dowglasmaia.financialtransaction.account.application.usecase.CreditAccountUseCase;
import com.dowglasmaia.financialtransaction.account.application.usecase.DebitAccountUseCase;
import com.dowglasmaia.financialtransaction.account.application.usecase.GetCurrentAccountUseCase;
import com.dowglasmaia.financialtransaction.account.application.usecase.GetCurrentAgencyUseCase;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.transaction.application.port.PixGateway;
import com.dowglasmaia.financialtransaction.transaction.domain.PixKey;
import com.dowglasmaia.financialtransaction.transaction.domain.PixKeyType;
import com.dowglasmaia.financialtransaction.transaction.domain.PixTransfer;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionStatus;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import com.dowglasmaia.financialtransaction.shared.error.UnprocessableEntityException;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InOrder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Pix = reserve -> call the bank (outside any transaction) -> confirm / compensate. */
@ExtendWith(MockitoExtension.class)
class MakePixTransferUseCaseTest {

    private static final PixTransfer VALID = new PixTransfer(new PixKey(PixKeyType.CPF, "82215344040"), new BigDecimal("25.00"));

    @Mock PixGateway gateway;
    @Mock GetCurrentAccountUseCase getCurrentAccount;
    @Mock GetCurrentAgencyUseCase getCurrentAgency;
    @Mock DebitAccountUseCase debitAccount;
    @Mock CreditAccountUseCase creditAccount;
    @Mock RecordTransactionUseCase recordTransaction;
    @Mock UpdateTransactionStatusUseCase updateStatus;

    MakePixTransferUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new MakePixTransferUseCase(gateway, getCurrentAccount, getCurrentAgency, debitAccount, creditAccount,
              recordTransaction, updateStatus, new TransactionTemplate(mock(PlatformTransactionManager.class)));
    }

    private void signedInCustomer() {
        when(getCurrentAccount.execute()).thenReturn(Account.restore("acc-1", null, null, "2022", null));
        when(getCurrentAgency.execute()).thenReturn("agency-id");
        when(recordTransaction.execute(TransactionType.PIX, "acc-1", new BigDecimal("25.00"), "agency-id", TransactionStatus.PENDING)).thenReturn("ledger-1");
    }

    @Test
    void reservesCallsTheBankThenConfirms() {
        signedInCustomer();

        useCase.execute(VALID);

        InOrder order = inOrder(debitAccount, recordTransaction, gateway, updateStatus);
        order.verify(debitAccount).execute("acc-1", new BigDecimal("25.00"));
        order.verify(recordTransaction).execute(TransactionType.PIX, "acc-1", new BigDecimal("25.00"), "agency-id", TransactionStatus.PENDING);
        order.verify(gateway).send();
        order.verify(updateStatus).execute("ledger-1", TransactionStatus.COMPLETED);
        verifyNoInteractions(creditAccount);
    }

    @Test
    void aFailureOfTheBankGivesTheMoneyBackAndFailsTheLedgerRow() {
        signedInCustomer();
        doThrow(new IllegalStateException("maiabank down")).when(gateway).send();

        assertThatThrownBy(() -> useCase.execute(VALID))
              .isInstanceOf(UnprocessableEntityException.class).hasMessage("Fail send Pix.");

        verify(creditAccount).execute("acc-1", new BigDecimal("25.00"));
        verify(updateStatus).execute("ledger-1", TransactionStatus.FAILED);
        verify(updateStatus, never()).execute("ledger-1", TransactionStatus.COMPLETED);
    }

    @Test
    void anInvalidKeyIsRejectedWithTheTypeInTheMessageAndNothingMoves() {
        PixTransfer bad = new PixTransfer(new PixKey(PixKeyType.CPF, "12345678900"), BigDecimal.TEN);

        assertThatThrownBy(() -> useCase.execute(bad))
              .isInstanceOf(UnprocessableEntityException.class)
              .hasMessage("Pix key does not exist or is invalid for the specified type: cpf");

        verifyNoInteractions(getCurrentAccount, debitAccount, recordTransaction, gateway, updateStatus, creditAccount);
    }
}
