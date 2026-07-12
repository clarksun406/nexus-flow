package com.nexusflow.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusflow.domain.merchant.MerchantApiKey;
import com.nexusflow.domain.merchant.MerchantCredentialRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiKeyAuthFilterTest {

    private MerchantCredentialRepository repository;
    private ApiKeyAuthFilter filter;

    @BeforeEach
    void setUp() {
        repository = mock(MerchantCredentialRepository.class);
        filter = new ApiKeyAuthFilter("global-key", repository, new ApiKeyHasher(), new ObjectMapper());
    }

    @Test
    void merchantApiKeySetsMerchantContext() throws Exception {
        when(repository.findActiveByKeyHash(eq(new ApiKeyHasher().hash("merchant-key-1")), any(Instant.class)))
                .thenReturn(Optional.of(MerchantApiKey.builder()
                        .merchantId("merchant-1")
                        .merchantCode("m-code-1")
                        .keyPrefix("mkey")
                        .build()));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/pay/order");
        request.addHeader("X-API-Key", "merchant-key-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(request.getAttribute(MerchantAuthContext.MERCHANT_ID_ATTRIBUTE)).isEqualTo("merchant-1");
        assertThat(request.getAttribute(MerchantAuthContext.MERCHANT_CODE_ATTRIBUTE)).isEqualTo("m-code-1");
        assertThat(request.getAttribute(MerchantAuthContext.AUTH_SOURCE_ATTRIBUTE)).isEqualTo("merchant-api-key");
    }

    @Test
    void globalFallbackKeyKeepsMigrationPathWithoutMerchantContext() throws Exception {
        when(repository.findActiveByKeyHash(any(), any(Instant.class))).thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/pay/order");
        request.addHeader("X-API-Key", "global-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(request.getAttribute(MerchantAuthContext.MERCHANT_ID_ATTRIBUTE)).isNull();
        assertThat(request.getAttribute(MerchantAuthContext.AUTH_SOURCE_ATTRIBUTE)).isEqualTo("global-api-key");
    }

    @Test
    void invalidKeyReturnsUnauthorized() throws Exception {
        when(repository.findActiveByKeyHash(any(), any(Instant.class))).thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/pay/order");
        request.addHeader("X-API-Key", "bad-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("NF-0004");
    }

    @Test
    void hashesPresentedApiKeyBeforeRepositoryLookup() throws Exception {
        when(repository.findActiveByKeyHash(any(), any(Instant.class))).thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/pay/order");
        request.addHeader("X-API-Key", "merchant-key-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(repository).findActiveByKeyHash(captor.capture(), any(Instant.class));
        assertThat(captor.getValue()).isEqualTo(new ApiKeyHasher().hash("merchant-key-1"));
    }

    @Test
    void merchantApiKeyRejectedOnOpsEndpoint() throws Exception {
        when(repository.findActiveByKeyHash(eq(new ApiKeyHasher().hash("merchant-key-1")), any(Instant.class)))
                .thenReturn(Optional.of(MerchantApiKey.builder()
                        .merchantId("merchant-1")
                        .merchantCode("m-code-1")
                        .keyPrefix("mkey")
                        .build()));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ops/dashboard");
        request.addHeader("X-API-Key", "merchant-key-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Merchant-scoped API keys cannot access this endpoint");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void globalKeyAllowedOnOpsEndpoint() throws Exception {
        when(repository.findActiveByKeyHash(any(), any(Instant.class))).thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ops/dashboard");
        request.addHeader("X-API-Key", "global-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(request.getAttribute(MerchantAuthContext.AUTH_SOURCE_ATTRIBUTE)).isEqualTo("global-api-key");
    }

    @Test
    void merchantApiKeySetsUserIdAttribute() throws Exception {
        when(repository.findActiveByKeyHash(eq(new ApiKeyHasher().hash("merchant-key-1")), any(Instant.class)))
                .thenReturn(Optional.of(MerchantApiKey.builder()
                        .merchantId("merchant-1")
                        .merchantCode("m-code-1")
                        .keyPrefix("mkey")
                        .build()));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/pay/order");
        request.addHeader("X-API-Key", "merchant-key-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        String userId = (String) request.getAttribute(MerchantAuthContext.USER_ID_ATTRIBUTE);
        assertThat(userId).isNotNull();
        assertThat(userId).isEqualTo(UserIdMapper.toUuid("merchant-1").toString());
    }

    @Test
    void globalKeySetsOpsUserIdAttribute() throws Exception {
        when(repository.findActiveByKeyHash(any(), any(Instant.class))).thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/pay/order");
        request.addHeader("X-API-Key", "global-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        String userId = (String) request.getAttribute(MerchantAuthContext.USER_ID_ATTRIBUTE);
        assertThat(userId).isEqualTo(UserIdMapper.OPS_USER_ID.toString());
    }

    @Test
    void sessionAuthPopulatesMerchantContextWithoutApiKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/pay/order");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("nexusflow.auth.userId", UUID.randomUUID().toString());
        session.setAttribute("nexusflow.auth.activeMerchantId", "550e8400-e29b-41d4-a716-446655440000");
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(request.getAttribute(MerchantAuthContext.AUTH_SOURCE_ATTRIBUTE)).isEqualTo("session");
        assertThat(request.getAttribute(MerchantAuthContext.MERCHANT_ID_ATTRIBUTE))
                .isEqualTo("550e8400-e29b-41d4-a716-446655440000");
    }
}
