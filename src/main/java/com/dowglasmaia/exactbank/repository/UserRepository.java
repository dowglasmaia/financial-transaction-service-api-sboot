package com.dowglasmaia.exactbank.repository;

import com.dowglasmaia.exactbank.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
}
