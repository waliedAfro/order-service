package com.orders.integration;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public class AbstractOrderIntegrationTest {

      @SuppressWarnings("resource")
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("order_test")
                    .withUsername("order_test")
                    .withPassword("order_test");

    protected static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("apache/kafka:4.1.0"));

    static { 
        POSTGRES.start(); 
        KAFKA.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        registry.add("spring.kafka.bootstrap-servers",KAFKA::getBootstrapServers);

        registry.add("spring.kafka.topics.inventory-reserved",() -> "it.inventory-reserved");
        registry.add("spring.kafka.topics.inventory-reservation-failed",() -> "it.inventory-reservation-failed");
        registry.add("spring.kafka.topics.payment-completed",() -> "it.payment-completed");
        registry.add("spring.kafka.topics.payment-failed",() -> "it.payment-failed");
        registry.add("spring.kafka.topics.order-created",() -> "it.order-created");
    }
}
