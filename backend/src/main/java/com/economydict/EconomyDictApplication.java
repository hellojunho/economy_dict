package com.economydict;

import com.economydict.config.SecretsJsonLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class EconomyDictApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(EconomyDictApplication.class);
        application.setDefaultProperties(SecretsJsonLoader.load());
        application.run(args);
    }
}
