package com.work.broker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SbjRabbitmqExampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(SbjRabbitmqExampleApplication.class, args);
	}

}
