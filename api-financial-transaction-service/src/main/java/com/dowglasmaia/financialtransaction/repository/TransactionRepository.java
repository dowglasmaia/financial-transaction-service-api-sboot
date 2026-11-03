package com.dowglasmaia.financialtransaction.repository;

import com.dowglasmaia.financialtransaction.entity.Transaction;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    /**
     * Statement rows of ONE account: only completed operations (pending/failed Pix reservations are not part of the
     * statement) and only the caller's own rows (spec BE-04).
     */
    List<Transaction> findByStatusAndAccountIdAndDateHourBetween(TransactionStatus status, String accountId,
                                                                OffsetDateTime initialDate, OffsetDateTime finalDate);

    /** A transaction by id, but only when it belongs to the given account (foreign ids behave as unknown ones). */
    Optional<Transaction> findByIdAndAccountId(String id, String accountId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update Transaction t set t.status = :status where t.id = :id")
    int updateStatus(@Param("id") String id, @Param("status") TransactionStatus status);

}
