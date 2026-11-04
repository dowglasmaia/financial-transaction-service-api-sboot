package com.dowglasmaia.financialtransaction.transaction.infrastructure.persistence;

import com.dowglasmaia.financialtransaction.transaction.domain.TransactionStatus;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, String> {

    /**
     * One page of COMPLETED rows of ONE account inside [start, end). The sort comes from the Pageable. A List (not a Page)
     * is returned on purpose: no count query; the count comes with the totals. When {@code filtered} is false the type
     * is ignored (it is always bound, to dodge untyped null parameters).
     */
    @Query("""
          select t from TransactionEntity t
          where t.accountId = :accountId and t.status = :status
            and t.dateHour >= :start and t.dateHour < :end
            and (:filtered = false or t.transactionType = :type)
          """)
    List<TransactionEntity> findPage(@Param("accountId") String accountId, @Param("status") TransactionStatus status,
                                     @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end,
                                     @Param("filtered") boolean filtered, @Param("type") TransactionType type, Pageable pageable);

    /** One aggregate: sums over the whole period, count over the rows matching the (optional) type. */
    @Query("""
          select new com.dowglasmaia.financialtransaction.transaction.infrastructure.persistence.TotalsRow(
                 coalesce(sum(case when t.transactionType = com.dowglasmaia.financialtransaction.transaction.domain.TransactionType.DEPOSIT
                                   then t.amount else 0 end), 0),
                 coalesce(sum(case when t.transactionType <> com.dowglasmaia.financialtransaction.transaction.domain.TransactionType.DEPOSIT
                                   then t.amount else 0 end), 0),
                 coalesce(sum(case when (:filtered = false or t.transactionType = :type) then 1L else 0L end), 0L))
          from TransactionEntity t
          where t.accountId = :accountId and t.status = :status
            and t.dateHour >= :start and t.dateHour < :end
          """)
    TotalsRow totals(@Param("accountId") String accountId, @Param("status") TransactionStatus status,
                     @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end,
                     @Param("filtered") boolean filtered, @Param("type") TransactionType type);

    /** Per Sao Paulo day (the zone conversion is done by PostgreSQL, so a late-evening entry stays on its own day). */
    @Query(value = """
          select cast((t.date_hour at time zone 'America/Sao_Paulo') as date) as day,
                 coalesce(sum(case when t.transaction_type = 'DEPOSIT' then t.amount else 0 end), 0) as incoming,
                 coalesce(sum(case when t.transaction_type <> 'DEPOSIT' then t.amount else 0 end), 0) as outgoing
          from transaction t
          where t.account_id = :accountId and t.status = :status
            and t.date_hour >= :start and t.date_hour < :end
          group by 1 order by 1
          """, nativeQuery = true)
    List<Object[]> dailyTotals(@Param("accountId") String accountId, @Param("status") String status,
                               @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    /** A transaction by id, but only when it belongs to the given account (foreign ids behave as unknown ones). */
    Optional<TransactionEntity> findByIdAndAccountId(String id, String accountId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update TransactionEntity t set t.status = :status where t.id = :id")
    int updateStatus(@Param("id") String id, @Param("status") TransactionStatus status);

}
