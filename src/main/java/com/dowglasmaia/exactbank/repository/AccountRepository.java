package com.dowglasmaia.exactbank.repository;

import com.dowglasmaia.exactbank.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {

    Optional<Account> findByNumber(String number);

    @Query("SELECT a FROM Account a WHERE a.agency.id = :agencyId AND a.number = :number")
    Optional<Account> findByAgencyAndNumber(@Param("agencyId") String agencyId, @Param("number") String number);


    @Query("SELECT a FROM Account a WHERE a.number = :number AND a.user.id = :userId")
    Optional<Account> findByNumberAndUser(@Param("number") String number, @Param("userId") String userId);
}
