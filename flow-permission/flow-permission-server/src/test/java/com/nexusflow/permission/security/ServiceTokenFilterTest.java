package com.nexusflow.permission.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceTokenFilterTest {

    @Test
    void allowsRequestWithConfiguredBearerToken() throws ServletException, IOException {
        ServiceTokenFilter filter = new ServiceTokenFilter(true, "secret-token");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/permission/list");
        request.addHeader("Authorization", "Bearer secret-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsRequestWithMissingToken() throws ServletException, IOException {
        ServiceTokenFilter filter = new ServiceTokenFilter(true, "secret-token");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/permission/list");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void canBeDisabledForLocalDevelopment() throws ServletException, IOException {
        ServiceTokenFilter filter = new ServiceTokenFilter(false, "");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/permission/list");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
