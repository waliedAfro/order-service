package com.orders.outbox;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

        private static final int BATCH_SIZE = 100;
        private final OutboxEventRepository outboxEventRepository;
        private final KafkaTemplate<String, String> kafkaTemplate;

        @Scheduled(fixedDelayString = "${outbox.scheduler.delay:5000}")
        public void processEvents() {

                log.debug("Starting outbox event processing");

                List<OutboxEvent> events = outboxEventRepository.findEventsForProcessing(
                                Instant.now(),
                                PageRequest.of(0, BATCH_SIZE));

                if (events.isEmpty()) {

                        log.debug("No outbox events ready for processing");

                        return;
                }

                log.info("Found {} outbox events ready for processing", events.size());

                for (OutboxEvent event : events) {
                        publish(event);
                }
        }

        private void publish(OutboxEvent event) {
                try {

                        // -----------------------------------------
                        // 1. Mark event as PROCESSING
                        // -----------------------------------------

                        event.setStatus(OutboxStatus.PROCESSING);

                        outboxEventRepository.save(event);

                        log.debug("Processing outbox event: eventId={}, retryCount={}",
                                        event.getId(),event.getRetryCount());
                        // -----------------------------------------
                        // 2. Publish to Kafka
                        // -----------------------------------------

                        kafkaTemplate.send(event.getTopic(),event.getEventKey(),event.getPayload()).get();

                        // -----------------------------------------
                        // 3. Mark event as PUBLISHED
                        // -----------------------------------------

                        event.setStatus(OutboxStatus.PUBLISHED);

                        event.setPublishedAt(Instant.now());

                        event.setNextRetryAt(null);

                        event.setLastError(null);

                        outboxEventRepository.save(event);

                        log.info("Outbox event published: eventId={}, type={}, topic={}",
                                        event.getId(),event.getEventType(),event.getTopic());

                } catch (Exception ex) {

                        handleFailure(event, ex);
                }
        }

       private void handleFailure(OutboxEvent event,Exception ex) {

        int retryCount = event.getRetryCount() + 1;

        event.setRetryCount(retryCount);

        event.setStatus(OutboxStatus.FAILED);

        event.setLastError(ex.getMessage());

        long delay =calculateRetryDelay(retryCount);

        event.setNextRetryAt(Instant.now().plusSeconds(delay));

        outboxEventRepository.save(event);

        log.error(
                "Failed to publish outbox event: " +
                "eventId={}, retryCount={}, nextRetryAt={}",
                event.getId(),
                retryCount,
                event.getNextRetryAt(),
                ex
        );
    }

    private long calculateRetryDelay(int retryCount) {

        return switch (retryCount) {

            case 1 -> 10;

            case 2 -> 30;

            case 3 -> 60;

            case 4 -> 120;

            default -> 300;
        };
    }

}
