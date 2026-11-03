package com.dowglasmaia.financialtransaction.transaction.infrastructure.persistence;

import com.dowglasmaia.financialtransaction.transaction.application.port.TransactionRepository;
import com.dowglasmaia.financialtransaction.transaction.domain.Transaction;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Implements the ledger port over Spring Data; the only place that maps TransactionEntity to the domain and back. */
@Component
class TransactionRepositoryAdapter implements TransactionRepository {

    private final TransactionJpaRepository jpa;

    TransactionRepositoryAdapter(TransactionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Transaction save(Transaction transaction) {
        return toDomain(jpa.save(toEntity(transaction)));
    }

    @Override
    public List<Transaction> findByStatusAndAccountIdAndDateHourBetween(TransactionStatus status, String accountId,
                                                                        OffsetDateTime initialDate, OffsetDateTime finalDate) {
        return jpa.findByStatusAndAccountIdAndDateHourBetween(status, accountId, initialDate, finalDate).stream()
              .map(TransactionRepositoryAdapter::toDomain)
              .toList();
    }

    @Override
    public Optional<Transaction> findByIdAndAccountId(String id, String accountId) {
        return jpa.findByIdAndAccountId(id, accountId).map(TransactionRepositoryAdapter::toDomain);
    }

    @Override
    public void updateStatus(String id, TransactionStatus status) {
        jpa.updateStatus(id, status);
    }

    private static Transaction toDomain(TransactionEntity entity) {
        return Transaction.restore(entity.getId(), entity.getTransactionType(), entity.getAccountId(), entity.getAmount(),
              entity.getAgencyId(), entity.getStatus(), entity.getDateHour());
    }

    private static TransactionEntity toEntity(Transaction transaction) {
        return TransactionEntity.builder()
              .id(transaction.getId())
              .transactionType(transaction.getTransactionType())
              .accountId(transaction.getAccountId())
              .amount(transaction.getAmount())
              .agencyId(transaction.getAgencyId())
              .status(transaction.getStatus())
              .dateHour(transaction.getDateHour())
              .build();
    }
}
