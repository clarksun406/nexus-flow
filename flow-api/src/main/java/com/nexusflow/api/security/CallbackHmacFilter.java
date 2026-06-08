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
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Filter that verifies HMAC-SHA256 signatures on channel callback endpoints.
 *
 * Wraps the request with {@link ContentCachingRequestWrapper} so the body
 * can be read multiple times (once for verification, once by the controller).
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
        // Wrap the request so the body can be re-read by the controller
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);

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

        // Force the body to be cached
        wrappedRequest.getInputStream().readAllBytes();
        byte[] bodyBytes = wrappedRequest.getContentAsByteArray();
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
}
