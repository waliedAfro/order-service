                         ┌──────────────┐
                         │ Order Service│
                         └──────┬───────┘
                                │
                         OrderCreatedEvent
                                │
                                ▼
                          ┌───────────┐
                          │  Kafka    │
                          └─────┬─────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │ Inventory       │
                       │ Service         │
                       └────────┬────────┘
                                │
                    ┌───────────┴───────────┐
                    │                       │
             Reserved                  Failed
                    │                       │
                    ▼                       ▼
          InventoryReserved       InventoryReservationFailed
                    │                       │
                    ▼                       ▼
               Order Service          Order Service
                    │                       │
                    ▼                       ▼
          INVENTORY_RESERVED             CANCELLED
                    │
                    ▼
            PaymentRequested
                    │
                    ▼
             Payment Service
                    │
             ┌──────┴──────┐
             │             │
          SUCCESS        FAILED
             │             │
             ▼             ▼
           PAID      PAYMENT_FAILED
                           │
                           ▼
                    Release Inventory
                           │
                           ▼
                       CANCELLED