package com.acme.salary;

import org.springframework.boot.SpringApplication;

public class TestSalaryApplication {

	public static void main(String[] args) {
		SpringApplication.from(SalaryApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
