package com.dowglasmaia.financialtransaction.transaction.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dowglasmaia.financialtransaction.account.application.usecase.DebitAccountUseCase;
import com.dowglasmaia.financialtransaction.account.application.usecase.GetCurrentAccountUseCase;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.transaction.application.port.RechargeEventPublisher;
import com.dowglasmaia.financialtransaction.transaction.domain.MobileRecharge;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import com.dowglasmaia.financialtransaction.exeptions.UnprocessableEntityExeption;
import java.math.BigDecimal;
import org.mockito.InOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequestMobileRechargeUseCaseTest {

    @Mock GetCurrentAccountUseCase getCurrentAccount;
    @Mock DebitAccountUseCase debitAccount;
    @Mock SaveTransactionUseCase saveTransaction;
    @Mock RechargeEventPublisher publisher;

    private RequestMobileRechargeUseCase useCase() {
        return new RequestMobileRechargeUseCase(getCurrentAccount, debitAccount, saveTransaction, publisher);
    }

    @Test
    void debitsWritesTheLedgerRowAndPublishesTheEventInThatOrder() {
        when(getCurrentAccount.execute()).thenReturn(Account.restore("acc-1", null, null, "2022", null));
        MobileRecharge recharge = new MobileRecharge(11, 998877665, new BigDecimal("50.00"), "VIVO");

        useCase().execute(recharge);

        InOrder order = inOrder(debitAccount, saveTransaction, publisher);
        order.verify(debitAccount).execute("acc-1", new BigDecimal("50.00"));
        order.verify(saveTransaction).execute(TransactionType.PHONE_RECHARGE, "acc-1", new BigDecimal("50.00"), null);
        order.verify(publisher).publish(recharge);
    }

    @Test
    void anInvalidPhoneIsRejectedBeforeAnyMoneyMoves() {
        assertThatThrownBy(() -> useCase().execute(new MobileRecharge(11, 12345, BigDecimal.TEN, "TIM")))
              .isInstanceOf(UnprocessableEntityExeption.class).hasMessage("Invalid cell phone numer");

        verifyNoInteractions(getCurrentAccount, debitAccount, saveTransaction, publisher);
    }
}
