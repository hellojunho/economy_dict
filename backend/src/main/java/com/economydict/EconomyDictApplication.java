package com.economydict;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class EconomyDictApplication {
    public static void main(String[] args) {
        SpringApplication.run(EconomyDictApplication.class, args);
    }
}
