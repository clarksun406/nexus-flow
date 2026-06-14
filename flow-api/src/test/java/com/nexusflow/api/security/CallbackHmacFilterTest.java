package com.nexusflow.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CallbackHmacFilterTest {

    @Test
    void validSignatureLeavesBodyReadableForController() throws Exception {
        String body = "{\"order_id\":\"order-1\",\"amount\":\"100\"}";
        String secret = "secret";
        CallbackHmacFilter filter = new CallbackHmacFilter(
                Map.of("SELF_HOSTED_NODE", secret),
                new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/callback/SELF_HOSTED_NODE/payment");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        request.addHeader("X-Signature", HmacSignatureVerifier.sign(body, secret));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> downstreamBody = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) ->
                downstreamBody.set(new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8)));

        assertEquals(200, response.getStatus());
        assertEquals(body, downstreamBody.get());
    }
}
