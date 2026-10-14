package com.orders.integration;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;

public class OrderInfrastructureIntegrationTest extends AbstractOrderIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void shouldConnectToPostgreSQLContainer() {
        String databaseName = jdbcTemplate.queryForObject(
                "SELECT current_database()", String.class);

        assertThat(databaseName).isEqualTo("order_test");
    }

    @Test
    void shouldPublishMessageToKafkaContainer() throws Exception {
        String topic = "it.infrastructure-smoke";
        String key = UUID.randomUUID().toString();
        String message = "order-service-test";

        var result = kafkaTemplate.send(topic, key, message)
                .get(10, TimeUnit.SECONDS);

        assertThat(result.getRecordMetadata().topic())
                .isEqualTo(topic);
    }

}
