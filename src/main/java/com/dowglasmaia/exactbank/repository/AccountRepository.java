package com.dowglasmaia.exactbank.repository;

import com.dowglasmaia.exactbank.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {

    Optional<Account>findByNumber(String number);
}
