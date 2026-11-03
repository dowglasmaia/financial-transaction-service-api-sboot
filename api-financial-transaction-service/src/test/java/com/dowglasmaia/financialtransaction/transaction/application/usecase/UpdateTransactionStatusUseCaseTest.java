package com.dowglasmaia.financialtransaction.transaction.application.usecase;

import static org.mockito.Mockito.verify;

import com.dowglasmaia.financialtransaction.transaction.application.port.TransactionRepository;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateTransactionStatusUseCaseTest {

    @Mock TransactionRepository ledger;

    @Test
    void changesTheStatusThroughThePort() {
        new UpdateTransactionStatusUseCase(ledger).execute("t-1", TransactionStatus.FAILED);

        verify(ledger).updateStatus("t-1", TransactionStatus.FAILED);
    }
}
