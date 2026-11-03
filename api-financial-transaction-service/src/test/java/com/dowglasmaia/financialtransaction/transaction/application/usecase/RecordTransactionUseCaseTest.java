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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordTransactionUseCaseTest {

    @Mock TransactionRepository ledger;

    @Test
    void storesTheEntryWithTheGivenStatusAndReturnsItsId() {
        when(ledger.save(any(Transaction.class))).thenAnswer(call -> {
            Transaction t = call.getArgument(0);
            return Transaction.restore("t-1", t.getTransactionType(), t.getAccountId(), t.getAmount(), t.getAgencyId(), t.getStatus(), t.getDateHour());
        });

        String id = new RecordTransactionUseCase(ledger).execute(TransactionType.PIX, "acc-1", new BigDecimal("12.50"), "agency-1", TransactionStatus.PENDING);

        assertThat(id).isEqualTo("t-1");
        ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
        verify(ledger).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(saved.getValue().getTransactionType()).isEqualTo(TransactionType.PIX);
        assertThat(saved.getValue().getAccountId()).isEqualTo("acc-1");
        assertThat(saved.getValue().getAgencyId()).isEqualTo("agency-1");
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("12.50");
        assertThat(saved.getValue().getDateHour()).isNotNull();
    }
}
