package com.dowglasmaia.financialtransaction.transaction.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.dowglasmaia.financialtransaction.account.application.usecase.GetCurrentAccountUseCase;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.transaction.application.port.TransactionRepository;
import com.dowglasmaia.financialtransaction.transaction.domain.Transaction;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionStatus;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import com.dowglasmaia.financialtransaction.shared.error.ObjectNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetTransactionByIdUseCaseTest {

    @Mock TransactionRepository ledger;
    @Mock GetCurrentAccountUseCase getCurrentAccount;

    @Test
    void returnsTheEntryOnlyWhenItBelongsToTheCallersAccount() {
        when(getCurrentAccount.execute()).thenReturn(Account.restore("acc-1", null, null, "2022", null));
        Transaction row = Transaction.restore("t-1", TransactionType.PIX, "acc-1", BigDecimal.TEN, "agency-1", TransactionStatus.COMPLETED, null);
        when(ledger.findByIdAndAccountId("t-1", "acc-1")).thenReturn(Optional.of(row));
        when(ledger.findByIdAndAccountId("t-2", "acc-1")).thenReturn(Optional.empty());

        GetTransactionByIdUseCase useCase = new GetTransactionByIdUseCase(ledger, getCurrentAccount);

        assertThat(useCase.execute("t-1")).isSameAs(row);
        assertThatThrownBy(() -> useCase.execute("t-2"))
              .isInstanceOf(ObjectNotFoundException.class).hasMessage("Transaction not found");
    }
}
