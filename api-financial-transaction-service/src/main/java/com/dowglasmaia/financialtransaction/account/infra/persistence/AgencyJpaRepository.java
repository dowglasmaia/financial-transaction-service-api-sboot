package com.dowglasmaia.financialtransaction.account.infra.persistence;

import com.dowglasmaia.financialtransaction.account.infra.persistence.AgencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgencyJpaRepository extends JpaRepository<AgencyEntity, String> {

    Optional<AgencyEntity>findByNumber(String number);
}
