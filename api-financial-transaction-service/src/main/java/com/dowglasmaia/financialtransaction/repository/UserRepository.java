package com.dowglasmaia.financialtransaction.repository;

import com.dowglasmaia.financialtransaction.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
}
