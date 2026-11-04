package com.dowglasmaia.financialtransaction.transaction.infrastructure.persistence;

import com.dowglasmaia.financialtransaction.transaction.application.port.TransactionRepository;
import com.dowglasmaia.financialtransaction.transaction.domain.DailyTotal;
import com.dowglasmaia.financialtransaction.transaction.domain.StatementPeriod;
import com.dowglasmaia.financialtransaction.transaction.domain.StatementQuery;
import com.dowglasmaia.financialtransaction.transaction.domain.StatementTotals;
import com.dowglasmaia.financialtransaction.transaction.domain.Transaction;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionStatus;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    public List<Transaction> findStatementRows(String accountId, StatementQuery query) {
        Sort.Direction direction = query.ascending() ? Sort.Direction.ASC : Sort.Direction.DESC;
        String property = query.sort() == StatementQuery.Sort.AMOUNT ? "amount" : "dateHour";
        // the id as a last key keeps the pages stable when two rows tie
        Sort sort = Sort.by(direction, property).and(Sort.by(direction, "id"));
        StatementPeriod period = query.period();
        return jpa.findPage(accountId, TransactionStatus.COMPLETED, period.start(), period.endExclusive(),
                    query.type() != null, typeOrAny(query.type()), PageRequest.of(query.page(), query.size(), sort)).stream()
              .map(TransactionRepositoryAdapter::toDomain)
              .toList();
    }

    @Override
    public StatementTotals totals(String accountId, StatementPeriod period, TransactionType type) {
        TotalsRow row = jpa.totals(accountId, TransactionStatus.COMPLETED, period.start(), period.endExclusive(),
              type != null, typeOrAny(type));
        return new StatementTotals(row.sumIncoming(), row.sumOutgoing(), row.matching());
    }

    @Override
    public List<DailyTotal> dailyTotals(String accountId, StatementPeriod period) {
        return jpa.dailyTotals(accountId, TransactionStatus.COMPLETED.name(), period.start(), period.endExclusive()).stream()
              .map(r -> new DailyTotal(toLocalDate(r[0]), (BigDecimal) r[1], (BigDecimal) r[2]))
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

    /** The type parameter is always bound (a typed null is a trap in PostgreSQL); when there is no filter it is never read. */
    private static TransactionType typeOrAny(TransactionType type) {
        return type == null ? TransactionType.DEPOSIT : type;
    }

    private static LocalDate toLocalDate(Object value) {
        return value instanceof java.sql.Date date ? date.toLocalDate() : (LocalDate) value;
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
