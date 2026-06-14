package com.nexusflow.api.config;

import com.nexusflow.application.GasBankFundingService;
import com.nexusflow.domain.gas.GasBank;
import com.nexusflow.domain.gas.GasEstimator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GasBankFundingConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(GasBankFundingConfig.class);

    @Test
    void fundingServiceIsNotRegisteredWithoutGasBank() {
        contextRunner
                .withBean(GasEstimator.class, () -> mock(GasEstimator.class))
                .run(context -> assertThat(context).doesNotHaveBean(GasBankFundingService.class));
    }

    @Test
    void fundingServiceIsRegisteredWhenGasBankAndEstimatorExist() {
        contextRunner
                .withBean(GasBank.class, () -> mock(GasBank.class))
                .withBean(GasEstimator.class, () -> mock(GasEstimator.class))
                .run(context -> assertThat(context).hasSingleBean(GasBankFundingService.class));
    }
}
