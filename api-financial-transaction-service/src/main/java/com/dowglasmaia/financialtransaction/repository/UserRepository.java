package com.dowglasmaia.financialtransaction.repository;

import com.dowglasmaia.financialtransaction.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByExternalSubject(String externalSubject);
}
