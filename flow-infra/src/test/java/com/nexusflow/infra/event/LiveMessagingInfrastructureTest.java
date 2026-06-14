package com.nexusflow.infra.event;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPool;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Opt-in live smoke checks for messaging dependencies. They are skipped by
 * default and only run when LIVE_* environment variables are configured.
 */
class LiveMessagingInfrastructureTest {

    @Test
    void redisLiveIdempotencySmoke() {
        String host = requireEnv("LIVE_REDIS_HOST");
        int port = intEnvOrDefault("LIVE_REDIS_PORT", 6379);

        try (JedisPool pool = new JedisPool(host, port)) {
            RedisProcessedEventStore store = new RedisProcessedEventStore(pool, 30);
            String eventId = "live-smoke-" + UUID.randomUUID();

            assertTrue(store.markProcessed(eventId), "first Redis idempotency write should win");
            assertFalse(store.markProcessed(eventId), "duplicate Redis idempotency write should be rejected");
        }
    }

    @Test
    void kafkaLiveProducerSmoke() throws Exception {
        String bootstrapServers = requireEnv("LIVE_KAFKA_BOOTSTRAP_SERVERS");
        String topic = envOrDefault("LIVE_KAFKA_TOPIC", "nexusflow.live.smoke");
        String key = "live-smoke-" + UUID.randomUUID();
        String payload = "{\"event_type\":\"live.smoke\",\"event_id\":\"" + key + "\"}";

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(kafkaProperties(bootstrapServers))) {
            RecordMetadata metadata = producer
                    .send(new ProducerRecord<>(topic, key, payload))
                    .get(20, TimeUnit.SECONDS);
            assertEquals(topic, metadata.topic());
        }
    }

    private static Properties kafkaProperties(String bootstrapServers) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "10000");
        properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000");
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "20000");
        return properties;
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        assumeTrue(hasText(value), "Set " + name + " to run this live smoke test");
        return value;
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return hasText(value) ? value : defaultValue;
    }

    private static int intEnvOrDefault(String name, int defaultValue) {
        String value = System.getenv(name);
        return hasText(value) ? Integer.parseInt(value) : defaultValue;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
