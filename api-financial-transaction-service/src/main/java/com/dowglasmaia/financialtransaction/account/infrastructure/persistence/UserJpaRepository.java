package com.dowglasmaia.financialtransaction.account.infrastructure.persistence;

import com.dowglasmaia.financialtransaction.account.infrastructure.persistence.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserEntity, String> {

    Optional<UserEntity> findByExternalSubject(String externalSubject);
}
