package com.nexusflow.api.config;

import com.nexusflow.application.GasBankFundingService;
import com.nexusflow.domain.gas.GasBank;
import com.nexusflow.domain.gas.GasEstimator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GasBankFundingConfig {

    @Bean
    @ConditionalOnBean({GasBank.class, GasEstimator.class})
    public GasBankFundingService gasBankFundingService(GasBank gasBank, GasEstimator gasEstimator) {
        return new GasBankFundingService(gasBank, gasEstimator);
    }
}
