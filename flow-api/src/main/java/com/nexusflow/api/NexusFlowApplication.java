package com.nexusflow.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.nexusflow")
// JPA entities and Spring Data repositories live in flow-infra (com.nexusflow.infra.persistence),
// outside the application package; without these annotations the default Spring Boot
// scanning (application package only) finds none of them and the context fails to start.
// This also makes the embedded-H2 'h2' profile boot with the same wiring.
@EntityScan(basePackages = "com.nexusflow.infra.persistence")
@EnableJpaRepositories(basePackages = "com.nexusflow.infra.persistence")
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan("com.nexusflow")
public class NexusFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusFlowApplication.class, args);
    }
}

