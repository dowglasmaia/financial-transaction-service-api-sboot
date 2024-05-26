package com.dowglasmaia.exactbank.repository;

import com.dowglasmaia.exactbank.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findByDateHourBetween(OffsetDateTime initialDate, OffsetDateTime finalDate);

}
