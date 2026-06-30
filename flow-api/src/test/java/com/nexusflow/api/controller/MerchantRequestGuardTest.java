package com.nexusflow.api.controller;

import com.nexusflow.api.security.MerchantAuthContext;
import com.nexusflow.common.NexusFlowException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MerchantRequestGuardTest {

    @Test
    void allowsRequestWhenAuthenticatedMerchantMatches() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(MerchantAuthContext.MERCHANT_ID_ATTRIBUTE, "merchant-1");

        assertThatCode(() -> MerchantRequestGuard.requireMatchingMerchant(request, "merchant-1"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsRequestWhenAuthenticatedMerchantDiffers() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(MerchantAuthContext.MERCHANT_ID_ATTRIBUTE, "merchant-1");

        assertThatThrownBy(() -> MerchantRequestGuard.requireMatchingMerchant(request, "merchant-2"))
                .isInstanceOf(NexusFlowException.class)
                .hasMessage("Request merchantId does not match authenticated merchant");
    }

    @Test
    void allowsRequestWithoutMerchantContextForGlobalFallbackKey() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatCode(() -> MerchantRequestGuard.requireMatchingMerchant(request, "merchant-2"))
                .doesNotThrowAnyException();
    }

    @Test
    void requireGlobalAccessAllowsGlobalApiKey() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(MerchantAuthContext.AUTH_SOURCE_ATTRIBUTE, "global-api-key");

        assertThatCode(() -> MerchantRequestGuard.requireGlobalAccess(request))
                .doesNotThrowAnyException();
    }

    @Test
    void requireGlobalAccessRejectsMerchantApiKey() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(MerchantAuthContext.AUTH_SOURCE_ATTRIBUTE, "merchant-api-key");

        assertThatThrownBy(() -> MerchantRequestGuard.requireGlobalAccess(request))
                .isInstanceOf(NexusFlowException.class)
                .hasMessage("Merchant-scoped API keys cannot access this endpoint");
    }

    @Test
    void requireGlobalAccessAllowsRequestWithNoAuthSource() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatCode(() -> MerchantRequestGuard.requireGlobalAccess(request))
                .doesNotThrowAnyException();
    }
}
