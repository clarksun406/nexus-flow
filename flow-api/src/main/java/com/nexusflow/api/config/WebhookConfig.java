package com.nexusflow.api.config;

import com.nexusflow.application.WebhookClient;
import com.nexusflow.infra.webhook.HttpWebhookClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for outbound webhook delivery.
 */
@Configuration
public class WebhookConfig {

    @Value("${nexusflow.webhook.hmac-secret:}")
    private String webhookSigningSecret;

    @Bean
    public RestTemplate webhookRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }

    @Bean
    public WebhookClient webhookClient(RestTemplate webhookRestTemplate) {
        return new HttpWebhookClient(webhookRestTemplate, webhookSigningSecret);
    }
}
