package com.roqhrcl.sealed;

import org.springframework.boot.SpringApplication;

public class TestSealedApplication {

	public static void main(String[] args) {
		SpringApplication.from(SealedApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
