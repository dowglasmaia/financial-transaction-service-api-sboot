package com.dowglasmaia.financialtransaction.transaction.application.usecase;

import com.dowglasmaia.financialtransaction.transaction.application.port.TransactionRepository;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateTransactionStatusUseCase {

    private final TransactionRepository ledger;

    public UpdateTransactionStatusUseCase(TransactionRepository ledger) {
        this.ledger = ledger;
    }

    @Transactional
    public void execute(String id, TransactionStatus status) {
        ledger.updateStatus(id, status);
    }
}
