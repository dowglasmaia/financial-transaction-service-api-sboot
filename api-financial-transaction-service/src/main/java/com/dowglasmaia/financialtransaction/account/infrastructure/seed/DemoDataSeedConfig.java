package com.dowglasmaia.financialtransaction.account.infrastructure.seed;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Demo/test seed - never active in production (profiles: local, test). */
@Configuration
@Profile({"local", "test"})
public class DemoDataSeedConfig {

    @Bean
    public boolean instantiateDataBase(DemoDataSeeder seeder) {
        seeder.instanciateTestDatabase();
        return true;
    }
}
