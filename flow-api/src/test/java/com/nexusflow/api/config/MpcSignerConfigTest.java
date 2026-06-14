package com.nexusflow.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusflow.domain.wallet.MpcSigner;
import com.nexusflow.infra.wallet.HttpMpcSigner;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class MpcSignerConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withUserConfiguration(MpcSignerConfig.class);

    @Test
    void httpMpcSignerIsNotRegisteredWithoutSignUrl() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(MpcSigner.class));
    }

    @Test
    void httpMpcSignerIsRegisteredWhenSignUrlIsConfigured() {
        contextRunner
                .withPropertyValues(
                        "nexusflow.mpc.http.sign-url=https://mpc.example/sign",
                        "nexusflow.mpc.http.api-key=api-key-1")
                .run(context -> {
                    assertThat(context).hasSingleBean(MpcSigner.class);
                    assertThat(context.getBean(MpcSigner.class)).isInstanceOf(HttpMpcSigner.class);
                });
    }
}
