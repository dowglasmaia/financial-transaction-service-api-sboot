package com.dowglasmaia.financialtransaction.repository;

import com.dowglasmaia.financialtransaction.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {

    Optional<Account> findByNumber(String number);

    @Query("SELECT a FROM Account a WHERE a.agency.id = :agencyId AND a.number = :number")
    Optional<Account> findByAgencyAndNumber(@Param("agencyId") String agencyId, @Param("number") String number);


    @Query("SELECT a FROM Account a WHERE a.number = :number AND a.user.id = :userId")
    Optional<Account> findByNumberAndUser(@Param("number") String number, @Param("userId") String userId);

    /**
     * The single balance rule: debit only when the balance covers the amount (balance >= amount), as ONE atomic
     * statement, so concurrent requests can never overdraw or lose an update. Returns the rows changed (0 = not enough).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Account a SET a.balance = a.balance - :amount WHERE a.id = :id AND a.balance >= :amount")
    int debit(@Param("id") String id, @Param("amount") BigDecimal amount);

    /** Atomic credit. Returns the rows changed (0 = unknown account). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Account a SET a.balance = a.balance + :amount WHERE a.id = :id")
    int credit(@Param("id") String id, @Param("amount") BigDecimal amount);
}
