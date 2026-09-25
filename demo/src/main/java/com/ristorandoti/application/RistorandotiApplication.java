package com.ristorandoti.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** {@code @EnableScheduling} attiva {@code OffertaLavoroCleanupJob} (pulizia delle offerte scadute). */
@SpringBootApplication
@EnableScheduling
public class RistorandotiApplication {

	public static void main(String[] args) {
		SpringApplication.run(RistorandotiApplication.class, args);
	}

}