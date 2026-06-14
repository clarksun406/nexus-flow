package com.nexusflow.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class BlockchainConfigTest {

    @Test
    void externalStubChannelBeansAreNotLoadedInProdProfile() throws Exception {
        assertHasNonProdProfile("bitMartAdapter");
        assertHasNonProdProfile("binancePayAdapter");
    }

    private static void assertHasNonProdProfile(String methodName) throws Exception {
        Method method = BlockchainConfig.class.getDeclaredMethod(methodName);
        Profile profile = method.getAnnotation(Profile.class);
        assertThat(profile).isNotNull();
        assertThat(Arrays.asList(profile.value())).contains("!prod");
    }
}
