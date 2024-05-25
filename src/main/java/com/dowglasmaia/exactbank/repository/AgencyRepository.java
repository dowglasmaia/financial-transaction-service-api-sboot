package com.dowglasmaia.exactbank.repository;

import com.dowglasmaia.exactbank.entity.Agency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgencyRepository extends JpaRepository<Agency, String> {

    Optional<Agency>findByNumber(String number);
}
