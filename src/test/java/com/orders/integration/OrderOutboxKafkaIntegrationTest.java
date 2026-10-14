package com.orders.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringDeserializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orders.domain.dto.OrderCreateRequest;
import com.orders.domain.dto.OrderItemRequest;
import com.orders.domain.dto.OrderResponse;
import com.orders.domain.model.OrderStatus;
import com.orders.kafka.producer.KafkaOutboxProcessor;
import com.orders.outbox.OutboxEvent;
import com.orders.outbox.OutboxEventRepository;
import com.orders.outbox.OutboxStatus;
//import com.orders.repository.OrderRepository;
import com.orders.service.OrderService;

@TestPropertySource(properties = "outbox.scheduler.delay=3600000")
public class OrderOutboxKafkaIntegrationTest extends AbstractOrderIntegrationTest {

    private static final int PARTITIONS = 1;

    @Autowired
    private OrderService orderService;

    //@Autowired
   // private OrderRepository orderRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private KafkaOutboxProcessor kafkaOutboxProcessor;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.topics.order-created}")
    private String orderCreatedTopic;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM order_items");
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM orders");
    }

    @Test
    void shouldPublishOrderCreatedEventAndMarkOutboxPublished()
            throws Exception {

        // Arrange: ensure the real Kafka topic exists.
        createTopicIfMissing();

        // Start the real consumer and establish its assignment before
        // creating the order, so older topic messages are not mistaken
        // for the event produced by this test.
        String consumerGroup = "order-outbox-it-" + UUID.randomUUID();

        Map<String, Object> consumerProperties = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, consumerGroup,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        try (Consumer<String, String> consumer = new KafkaConsumer<>(consumerProperties)) {

            consumer.subscribe(List.of(orderCreatedTopic));

            long assignmentDeadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();

            while (consumer.assignment().isEmpty()
                    && System.nanoTime() < assignmentDeadline) {
                consumer.poll(Duration.ofMillis(200));
            }

            assertThat(consumer.assignment())
                    .as("Kafka consumer should receive a partition assignment")
                    .isNotEmpty();

            UUID customerId = UUID.randomUUID();

            OrderCreateRequest request = new OrderCreateRequest(
                    customerId,
                    new BigDecimal("999.99"),
                    "QAR",
                    List.of(
                            new OrderItemRequest(
                                    UUID.randomUUID(),
                                    "Integration Test Product",
                                    2,
                                    new BigDecimal("12.50"))));

            // Act 1: real service writes order, item and outbox event.
            OrderResponse response = orderService.create(request);

            assertThat(response.status()).isEqualTo(OrderStatus.CREATED);
            assertThat(response.totalAmount())
                    .isEqualByComparingTo("25.00");

            OutboxEvent outboxEvent = outboxEventRepository.findAll()
                    .stream()
                    .filter(event -> response.id().equals(event.getAggregateId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Outbox event not found"));

            assertThat(outboxEvent.getStatus())
                    .isEqualTo(OutboxStatus.PENDING);

            // Act 2: invoke the actual production outbox processor.
            kafkaOutboxProcessor.processEvents();

            // Assert: consume the message from real Kafka.
            JsonNode publishedEvent = null;
            long messageDeadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();

            while (publishedEvent == null
                    && System.nanoTime() < messageDeadline) {

                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));

                for (var record : records) {
                    if (response.id().toString().equals(record.key())) {
                        publishedEvent = objectMapper.readTree(record.value());
                        break;
                    }
                }
            }

            assertThat(publishedEvent)
                    .as("Expected OrderCreatedEvent on Kafka")
                    .isNotNull();

            assertThat(publishedEvent.path("eventType").asText())
                    .isEqualTo("ORDER_CREATED");

            assertThat(publishedEvent.path("orderId").asText())
                    .isEqualTo(response.id().toString());

            assertThat(publishedEvent.path("orderNumber").asText())
                    .isEqualTo(response.orderNumber());

            assertThat(publishedEvent.path("customerId").asText())
                    .isEqualTo(customerId.toString());

            assertThat(publishedEvent.path("totalAmount").decimalValue())
                    .isEqualByComparingTo("25.00");

            assertThat(publishedEvent.path("items").size()).isEqualTo(1);

            // Reload from PostgreSQL: verify published status was persisted.
            OutboxEvent updatedEvent = outboxEventRepository
                    .findById(outboxEvent.getId())
                    .orElseThrow();

            assertThat(updatedEvent.getStatus())
                    .isEqualTo(OutboxStatus.PUBLISHED);

            assertThat(updatedEvent.getPublishedAt()).isNotNull();
            assertThat(updatedEvent.getProcessingAt()).isNull();
            assertThat(updatedEvent.getNextRetryAt()).isNull();
            assertThat(updatedEvent.getLastError()).isNull();
        }
    }

    private void createTopicIfMissing() throws Exception {
        Map<String, Object> adminProperties = Map.of(
                "bootstrap.servers", bootstrapServers);

        try (AdminClient admin = AdminClient.create(adminProperties)) {
            try {
                admin.createTopics(List.of(
                        new NewTopic(orderCreatedTopic, PARTITIONS, (short) 1)))
                        .all()
                        .get();
            } catch (java.util.concurrent.ExecutionException ex) {
                if (!(ex.getCause() instanceof TopicExistsException)) {
                    throw ex;
                }
            }
        }
    }
}
