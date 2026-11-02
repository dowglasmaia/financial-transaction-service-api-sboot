package com.dowglasmaia.financialtransaction.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;

/** One PostgreSQL container for the whole test run, shared by every Spring test context. */
public final class SharedPostgres {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:14.12-alpine3.18");

    static {
        POSTGRES.start();
    }

    private SharedPostgres() {
    }

    public static void register(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
