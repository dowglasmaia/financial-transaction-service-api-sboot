package com.dowglasmaia.exactbank.repository;

import com.dowglasmaia.exactbank.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
}
