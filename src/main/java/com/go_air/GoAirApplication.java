package com.go_air;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
public class GoAirApplication {

	public static void main(String[] args) {
		SpringApplication.run(GoAirApplication.class, args);
	}

}
