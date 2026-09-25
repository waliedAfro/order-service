package com.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class EnterpriseOrderPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(EnterpriseOrderPlatformApplication.class, args);
	}

}
