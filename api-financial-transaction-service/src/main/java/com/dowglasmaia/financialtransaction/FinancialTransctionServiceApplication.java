package com.dowglasmaia.financialtransaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class FinancialTransctionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinancialTransctionServiceApplication.class, args);
	}

}
