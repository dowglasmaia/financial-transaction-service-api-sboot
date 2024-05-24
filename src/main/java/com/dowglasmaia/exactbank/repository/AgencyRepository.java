package com.dowglasmaia.exactbank.repository;

import com.dowglasmaia.exactbank.entity.Agency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgencyRepository extends JpaRepository<Agency, String> {
}
