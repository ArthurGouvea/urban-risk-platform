package br.com.fiap.urbanrisk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UrbanriskApplication {

	public static void main(String[] args) {
		SpringApplication.run(UrbanriskApplication.class, args);
	}

}
