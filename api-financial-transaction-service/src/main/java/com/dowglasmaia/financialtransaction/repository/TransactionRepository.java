package com.dowglasmaia.financialtransaction.repository;

import com.dowglasmaia.financialtransaction.entity.Transaction;
import com.dowglasmaia.financialtransaction.entity.TransactionStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    /** Statement rows: only completed operations (pending/failed Pix reservations are not part of the statement). */
    List<Transaction> findByStatusAndDateHourBetween(TransactionStatusEnum status, OffsetDateTime initialDate, OffsetDateTime finalDate);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update Transaction t set t.status = :status where t.id = :id")
    int updateStatus(@Param("id") String id, @Param("status") TransactionStatusEnum status);

}
