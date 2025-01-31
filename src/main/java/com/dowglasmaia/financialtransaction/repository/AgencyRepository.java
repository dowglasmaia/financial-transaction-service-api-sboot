package com.dowglasmaia.financialtransaction.repository;

import com.dowglasmaia.financialtransaction.entity.Agency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgencyRepository extends JpaRepository<Agency, String> {

    Optional<Agency>findByNumber(String number);
}
