package com.nexusflow.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusflow.common.ApiResponse;
import com.nexusflow.common.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Filter that verifies HMAC-SHA256 signatures on channel callback endpoints.
 *
 * Wraps the request with a cached body so the payload can be read once for
 * verification and again by the controller.
 */
@Slf4j
public class CallbackHmacFilter extends OncePerRequestFilter {

    private static final String SIGNATURE_HEADER = "X-Signature";

    private final Map<String, String> channelSecrets; // channelId (uppercase) -> secret
    private final ObjectMapper objectMapper;

    public CallbackHmacFilter(Map<String, String> channelSecrets, ObjectMapper objectMapper) {
        this.channelSecrets = channelSecrets;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        byte[] bodyBytes = request.getInputStream().readAllBytes();
        CachedBodyRequest wrappedRequest = new CachedBodyRequest(request, bodyBytes);

        String path = wrappedRequest.getRequestURI();
        String channelId = extractChannelId(path);
        if (channelId == null) {
            reject(response, "Invalid callback path");
            return;
        }

        String secret = channelSecrets.get(channelId.toUpperCase());
        if (secret == null || secret.isBlank()) {
            log.warn("No HMAC secret configured for channel: {}", channelId);
            reject(response, "Unknown channel: " + channelId);
            return;
        }

        String signature = wrappedRequest.getHeader(SIGNATURE_HEADER);
        if (signature == null || signature.isBlank()) {
            log.warn("Missing X-Signature header from channel: {}", channelId);
            reject(response, "Missing signature");
            return;
        }

        String body = new String(bodyBytes, StandardCharsets.UTF_8);

        if (!HmacSignatureVerifier.verify(body, secret, signature)) {
            log.warn("Invalid HMAC signature from channel: {}", channelId);
            reject(response, "Invalid signature");
            return;
        }

        chain.doFilter(wrappedRequest, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/callback/");
    }

    private String extractChannelId(String path) {
        String[] parts = path.split("/");
        if (parts.length >= 3 && "callback".equals(parts[1])) {
            return parts[2];
        }
        return null;
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        log.warn("Callback rejected: {}", message);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.fail(ErrorCode.INVALID_SIGNATURE, message)));
    }

    private static final class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        private CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // Synchronous filter path; no async listener is needed.
                }

                @Override
                public int read() {
                    return input.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
