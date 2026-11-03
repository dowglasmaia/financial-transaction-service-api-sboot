package com.dowglasmaia.financialtransaction.transaction.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dowglasmaia.financialtransaction.transaction.application.port.TransactionRepository;
import com.dowglasmaia.financialtransaction.transaction.domain.Transaction;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionStatus;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class SaveTransactionUseCaseTest {

    @Mock TransactionRepository ledger;

    @Test
    void writesACompletedEntry() {
        when(ledger.save(any(Transaction.class))).thenAnswer(call -> call.getArgument(0));

        new SaveTransactionUseCase(new RecordTransactionUseCase(ledger)).execute(TransactionType.DEPOSIT, "acc-1", BigDecimal.TEN, "agency-1");

        ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
        verify(ledger).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(saved.getValue().getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
    }
}
