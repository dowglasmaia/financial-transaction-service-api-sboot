package com.dowglasmaia.financialtransaction.config.db_massa_test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.text.ParseException;

/** Demo/test seed - never active in production (profiles: local, test). */
@Configuration
@Profile({"local", "test"})
public class TestConfigDb {

    @Autowired
    private DBService dbService;

    @Bean
    public boolean instantiateDataBase() throws ParseException {
        dbService.instanciateTestDatabase();
        return true;
    }
}
