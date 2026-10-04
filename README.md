# Order Service

A production-oriented **Order Management Microservice** built with **Java, Spring Boot, PostgreSQL, and Apache Kafka**.

The service is responsible for creating and managing customer orders and integrates with other microservices using an **event-driven architecture** and the **Transactional Outbox Pattern**.

The project is designed to demonstrate reliable distributed-system patterns including:

* RESTful APIs
* Spring Boot
* PostgreSQL
* JPA / Hibernate
* Apache Kafka
* Event-Driven Architecture
* Transactional Outbox Pattern
* At-Least-Once Event Delivery
* Event Idempotency
* Retry and Resubmission
* Stale Event Recovery
* Domain-driven state transitions
* Exception handling
* Structured logging

---

## Architecture

The Order Service is one independent microservice in a larger order-processing ecosystem.

```text
                         ┌──────────────────────┐
                         │     Order Service    │
                         │                      │
Client ──REST──────────► │    OrderController   │
                         │          │           │
                         │          ▼           │
                         │    OrderService      │
                         │          │           │
                         │    ┌─────┴─────┐     │
                         │    ▼           ▼     │
                         │ PostgreSQL   Outbox  │
                         │              Event   │
                         └──────────────────────┘
                                         │
                                         │ Transactional Outbox
                                         ▼
                               KafkaOutboxProcessor
                                         │
                                         ▼
                                  KafkaTemplate
                                         │
                                         ▼
                                      Kafka
                                         │
                    ┌────────────────────┼────────────────────┐
                    ▼                    ▼                    ▼
                 Payment              Inventory             Audit
                 Service              Service              Service
                    │
                    │ PaymentCompletedEvent
                    │ PaymentFailedEvent
                    ▼
                               ┌──────────────┐
                               │    Kafka     │
                               └──────┬───────┘
                                      │
                                      ▼
                         ┌────────────────────────┐
                         │      Order Service     │
                         │                        │
                         │ PaymentCompleted       │
                         │ PaymentFailed Consumer  │
                         └───────────┬────────────┘
                                     │
                                     ▼
                               OrderService
                                     │
                                     ▼
                               ProcessedEvent
```

---

## Key Design: Transactional Outbox

The service uses the **Transactional Outbox Pattern** to avoid the dual-write problem.

Instead of:

```text
Save Order
     │
     └────► Send Kafka Message
```

the service performs both database operations inside the same transaction:

```text
BEGIN TRANSACTION
       │
       ├── Save Order
       │
       ├── Save Order Items
       │
       └── Save Outbox Event
       │
     COMMIT
       │
       ▼
KafkaOutboxProcessor
       │
       ▼
Kafka
```

This guarantees that an order and its corresponding event are committed together.

If the transaction fails:

```text
ROLLBACK
```

and neither the order nor the outbox event is committed.

---

# Order Creation Flow

When a client creates an order:

```text
POST /orders
      │
      ▼
OrderController
      │
      ▼
OrderService
      │
      ├── Validate request
      │
      ├── Generate order number
      │
      ├── Create Order
      │
      ├── Create OrderItems
      │
      ├── Calculate total
      │
      ├── Save Order
      │
      └── Create OutboxEvent
                │
                ▼
          PostgreSQL
```

The `OutboxEventPublisher` creates an `OrderCreatedEvent`, serializes it to JSON, and stores the serialized payload in the `outbox_events` table.

---

# Kafka Publishing Flow

The service does not send the Kafka message directly during order creation.

Instead:

```text
OrderService
     │
     ▼
OutboxEventPublisher
     │
     ▼
outbox_events
     │
     │ PENDING
     ▼
KafkaOutboxProcessor
     │
     ▼
KafkaTemplate<String, String>
     │
     ▼
Kafka
```

The Kafka payload is stored as a JSON string.

Example:

```json
{
  "eventId": "2f0a4b9e-8e8a-4d9b-9b6f-123456789abc",
  "eventType": "ORDER_CREATED",
  "occurredAt": "2026-10-02T18:00:00Z",
  "orderId": "c9c5f8f4-7a2a-4e8b-9d55-123456789abc",
  "orderNumber": "ORD-20261002-A1B2C3",
  "customerId": "d1c4e8f1-1234-4567-8901-123456789abc",
  "totalAmount": 250.00,
  "currency": "QAR",
  "items": [
    {
      "productId": "12345678-1234-1234-1234-123456789abc",
      "productName": "Product A",
      "quantity": 2,
      "unitPrice": 125.00,
      "subtotal": 250.00
    }
  ]
}
```

---

# Kafka Serialization

The producer uses:

```text
OrderCreatedEvent
        │
        ▼
ObjectMapper
        │
        ▼
JSON String
        │
        ▼
OutboxEvent.payload
        │
        ▼
KafkaTemplate<String, String>
        │
        ▼
StringSerializer
        │
        ▼
Kafka
```

Consumers use the reverse process:

```text
Kafka
  │
  ▼
StringDeserializer
  │
  ▼
JSON String
  │
  ▼
ObjectMapper
  │
  ▼
PaymentCompletedEvent
PaymentFailedEvent
```

---

# Outbox Event Lifecycle

Each outbox event has a lifecycle:

```text
             ┌───────────┐
             │  PENDING  │
             └─────┬─────┘
                   │
                   ▼
             ┌───────────┐
             │PROCESSING │
             └─────┬─────┘
                   │
             ┌─────┴──────┐
             │            │
          Success       Failure
             │            │
             ▼            ▼
       ┌───────────┐  ┌──────────┐
       │ PUBLISHED │  │  FAILED  │
       └───────────┘  └────┬─────┘
                           │
                     nextRetryAt
                           │
                           ▼
                      PROCESSING
```

## Outbox Statuses

```java
PENDING
PROCESSING
PUBLISHED
FAILED
```

### PENDING

Event has been stored but has not yet been published to Kafka.

### PROCESSING

The Kafka producer is currently attempting to publish the event.

### PUBLISHED

Kafka successfully acknowledged the event.

### FAILED

Publishing failed and the event is scheduled for retry.

---

# Retry and Resubmission

Kafka publishing failures are handled using retry scheduling.

Example retry sequence:

```text
Attempt 1 → +10 seconds
Attempt 2 → +30 seconds
Attempt 3 → +60 seconds
Attempt 4 → +120 seconds
Attempt 5+ → +300 seconds
```

The event stores:

```text
retryCount
nextRetryAt
lastError
```

This allows the service to retry failed events without losing them.

---

# Stale Processing Recovery

The service also protects against application crashes during event processing.

Example:

```text
PENDING
   │
   ▼
PROCESSING
processingAt = 10:00:00
   │
   │ Application crashes
   ▼
PROCESSING
   │
   │ timeout exceeded
   ▼
FAILED
   │
   ▼
Retry
```

The `processingAt` field records when processing started.

If an event remains in `PROCESSING` longer than the configured timeout, the `KafkaOutboxProcessor` can recover it and make it eligible for retry.

Configuration:

```properties
outbox.processing.timeout=300
```

The above configuration represents five minutes.

---

# Idempotency

The Order Service uses an idempotency mechanism for events received from Kafka.

Incoming payment events contain a unique:

```text
eventId
```

The service records processed events using:

```text
eventId
consumerName
```

Example:

```text
PaymentCompletedEvent
        │
        ▼
eventId
        │
        ▼
ProcessedEvent
        │
        ├── Already processed → Ignore
        │
        └── New event
              │
              ▼
         Update Order
              │
              ▼
        Record ProcessedEvent
```

This protects the service from duplicate Kafka deliveries.

---

# Payment Events

The Order Service consumes payment events from Kafka.

## PaymentCompletedEvent

When payment succeeds:

```text
Payment Service
      │
      ▼
PaymentCompletedEvent
      │
      ▼
Kafka
      │
      ▼
PaymentCompletedConsumer
      │
      ▼
OrderService
      │
      ▼
Order.markPaid()
```

The order transitions to:

```text
PAYMENT_PENDING
        │
        ▼
      PAID
```

---

## PaymentFailedEvent

When payment fails:

```text
Payment Service
      │
      ▼
PaymentFailedEvent
      │
      ▼
Kafka
      │
      ▼
PaymentFailedConsumer
      │
      ▼
OrderService
      │
      ▼
Order.markPaymentFailed()
```

The order transitions to the appropriate payment-failed state.

---

# Domain State Management

Order status changes are controlled by the `Order` domain model.

Instead of allowing services to directly manipulate the status:

```java
order.setStatus(OrderStatus.PAID);
```

the domain provides methods such as:

```java
order.markPaid();
```

and:

```java
order.markPaymentFailed(reason);
```

This allows the domain to enforce valid state transitions.

Example:

```text
PAYMENT_PENDING
       │
       ├── Payment completed
       │        ▼
       │       PAID
       │
       └── Payment failed
                ▼
         PAYMENT_FAILED
```

Invalid transitions are rejected using:

```text
InvalidOrderStateException
```

---

# Package Structure

```text
com.orders
│
├── common
│   └── events
│       ├── OrderCreatedEvent
│       ├── OrderCreatedItem
│       ├── PaymentCompletedEvent
│       └── PaymentFailedEvent
│
├── config
│   ├── KafkaConfig
│   └── KafkaConsumerConfig
│
├── controller
│   └── OrderController
│
├── domain
│   ├── dto
│   │   ├── OrderCreateRequest
│   │   ├── OrderItemRequest
│   │   ├── OrderItemResponse
│   │   ├── OrderResponse
│   │   └── OrderUpdateRequest
│   │
│   └── model
│       ├── Order
│       ├── OrderItem
│       └── OrderStatus
│
├── exception
│   ├── ApiError
│   ├── DuplicateOrderException
│   ├── GlobalExceptionHandler
│   ├── InvalidOrderStateException
│   └── OrderNotFoundException
│
├── idempotency
│   ├── ProcessedEvent
│   └── ProcessedEventRepository
│
├── kafka
│   ├── consumer
│   │   ├── PaymentCompletedConsumer
│   │   └── PaymentFailedConsumer
│   │
│   └── producer
│       └── KafkaOutboxProcessor
│
├── outbox
│   ├── OutboxEvent
│   ├── OutboxEventRepository
│   └── OutboxStatus
│
├── repository
│   └── OrderRepository
│
├── service
│   ├── OrderService
│   └── OrderServiceImpl
│
└── messaging
    └── publisher
        ├── EventPublisher
        └── OutboxEventPublisher
```

---

# Main Components

## `OrderController`

Exposes REST endpoin
