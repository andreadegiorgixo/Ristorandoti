package com.ristorandoti.application;

import org.springframework.boot.SpringApplication;

public class TestRistorandotiApplication {

	public static void main(String[] args) {
		SpringApplication.from(RistorandotiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
