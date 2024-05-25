package com.dowglasmaia.exactbank.repository;

import com.dowglasmaia.exactbank.entity.Account;
import com.dowglasmaia.exactbank.entity.Agency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {

    Optional<Account> findByNumber(String number);

    @Query("SELECT a FROM Account a WHERE a.agency = :agency AND a.number = :number")
    Optional<Account> findByAgencyAndNumber(@Param("agency") Agency agency, @Param("number") String number);
}
