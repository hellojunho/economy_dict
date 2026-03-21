package com.economydict;

import com.economydict.config.SecretsJsonLoader;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class EconomyDictApplication {
    public static void main(String[] args) {
        Map<String, Object> secrets = SecretsJsonLoader.load();
        SecretsJsonLoader.applySystemPropertyOverrides(secrets);
        SpringApplication application = new SpringApplication(EconomyDictApplication.class);
        application.setDefaultProperties(secrets);
        application.run(args);
    }
}
