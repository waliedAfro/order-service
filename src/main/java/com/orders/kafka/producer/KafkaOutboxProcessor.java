package com.orders.kafka.producer;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.orders.outbox.OutboxEvent;
import com.orders.outbox.OutboxEventRepository;
import com.orders.outbox.OutboxStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaOutboxProcessor {

        private static final int BATCH_SIZE = 100;

        private final OutboxEventRepository outboxEventRepository;
        private final KafkaTemplate<String, String> kafkaTemplate;

        @Value("${outbox.processing.timeout:300}")
        private long processingTimeoutSeconds;

        @Scheduled(fixedDelayString = "${outbox.scheduler.delay:5000}")
        public void processEvents() {

                log.debug("Starting outbox event processing");

                recoverStaleProcessingEvents();

                processReadyEvents();
        }

        private void recoverStaleProcessingEvents() {

                Instant staleBefore = Instant.now()
                                .minusSeconds(processingTimeoutSeconds);

                List<OutboxEvent> staleEvents = outboxEventRepository.findStaleProcessingEvents(
                                staleBefore,
                                PageRequest.of(0, BATCH_SIZE));

                if (staleEvents.isEmpty()) {

                        log.debug("No stale PROCESSING outbox events found");

                        return;
                }

                log.warn(
                                "Found {} stale PROCESSING outbox events",
                                staleEvents.size());

                for (OutboxEvent event : staleEvents) {

                        recoverStaleEvent(event);
                }
        }

        private void recoverStaleEvent(OutboxEvent event) {

                log.warn(
                                "Recovering stale outbox event. " +
                                                "eventId={}, processingAt={}, retryCount={}",
                                event.getId(),
                                event.getProcessingAt(),
                                event.getRetryCount());

                event.setStatus(OutboxStatus.FAILED);

                event.setProcessingAt(null);

                event.setLastError(
                                "Recovered stale PROCESSING event");

                event.setNextRetryAt(Instant.now());

                outboxEventRepository.save(event);
        }

        private void processReadyEvents() {

                Instant now = Instant.now();

                List<OutboxEvent> events = outboxEventRepository.findEventsForProcessing(
                                now,
                                PageRequest.of(0, BATCH_SIZE));

                if (events.isEmpty()) {

                        log.debug(
                                        "No outbox events ready for processing");

                        return;
                }

                log.info(
                                "Found {} outbox events ready for processing",
                                events.size());

                for (OutboxEvent event : events) {

                        publish(event);
                }
        }

        private void publish(OutboxEvent event) {

                try {

                        markProcessing(event);

                        log.debug(
                                        "Processing outbox event: " +
                                                        "eventId={}, type={}, retryCount={}",
                                        event.getId(),
                                        event.getEventType(),
                                        event.getRetryCount());

                        kafkaTemplate.send(
                                        event.getTopic(),
                                        event.getEventKey(),
                                        event.getPayload())
                                        .get();

                        markPublished(event);

                } catch (Exception ex) {

                        handleFailure(event, ex);
                }
        }

        private void markProcessing(OutboxEvent event) {

                event.setStatus(OutboxStatus.PROCESSING);

                event.setProcessingAt(Instant.now());

                outboxEventRepository.save(event);
        }

        private void markPublished(OutboxEvent event) {

                event.setStatus(OutboxStatus.PUBLISHED);

                event.setPublishedAt(Instant.now());

                event.setProcessingAt(null);

                event.setNextRetryAt(null);

                event.setLastError(null);

                outboxEventRepository.save(event);

                log.info(
                                "Outbox event published: " +
                                                "eventId={}, type={}, topic={}",
                                event.getId(),
                                event.getEventType(),
                                event.getTopic());
        }

        private void handleFailure(
                        OutboxEvent event,
                        Exception ex) {

                int retryCount = event.getRetryCount() + 1;

                event.setRetryCount(retryCount);

                event.setStatus(OutboxStatus.FAILED);

                event.setProcessingAt(null);

                event.setLastError(
                                ex.getMessage());

                long delay = calculateRetryDelay(retryCount);

                event.setNextRetryAt(
                                Instant.now().plusSeconds(delay));

                outboxEventRepository.save(event);

                log.error(
                                "Failed to publish outbox event: " +
                                                "eventId={}, retryCount={}, nextRetryAt={}",
                                event.getId(),
                                retryCount,
                                event.getNextRetryAt(),
                                ex);
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
