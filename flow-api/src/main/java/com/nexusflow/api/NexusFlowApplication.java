package com.nexusflow.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.nexusflow")
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan("com.nexusflow")
public class NexusFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusFlowApplication.class, args);
    }
}
