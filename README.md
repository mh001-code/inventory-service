# inventory-service

[![CI](https://github.com/mh001-code/inventory-service/actions/workflows/ci.yml/badge.svg)](https://github.com/mh001-code/inventory-service/actions/workflows/ci.yml)

Stock management service of the [Order Processing System](https://github.com/mh001-code) — a microservices portfolio project demonstrating event-driven architecture, idempotency, and clean hexagonal design.

## Overview

The `inventory-service` reacts to order events published by `order-service` and manages product stock accordingly. It never communicates directly with other services — all integration happens through RabbitMQ.

```
order-service → order.created   → RabbitMQ → inventory-service (reserve stock)
             → order.cancelled  →           → inventory-service (release stock)
```

## Architecture

Hexagonal (ports-and-adapters) architecture:

```
com.orderprocessing.inventory.service
├── domain/         # Product, StockMovement, StockMovementType, domain exceptions
├── application/    # Use cases (port/in), repository interfaces (port/out)
├── infrastructure/ # JPA adapters, RabbitMQ consumer, Spring config
└── api/            # REST controllers, DTOs, global exception handler
```

## Tech Stack

| Technology | Role |
|---|---|
| Java 17 | Language |
| Spring Boot 3.5 | Framework |
| Spring AMQP | RabbitMQ consumer |
| PostgreSQL 16 | Persistence |
| Flyway | DB migrations |
| JUnit 5 + Mockito | Unit tests |
| Testcontainers | Integration tests |
| Docker | Containerization |
| GitHub Actions | CI/CD |

## Endpoints

| Method | Route | Description | Status |
|---|---|---|---|
| `GET` | `/products` | List all products | 200 |
| `GET` | `/products/{id}` | Get product and current stock | 200 / 404 |
| `POST` | `/products` | Register a new product with initial stock | 201 / 409 |
| `PUT` | `/products/{id}/stock` | Manually adjust stock (replenishment) | 200 / 404 |
| `GET` | `/health` | Health check | 200 |

### Create Product — Example

```bash
curl -X POST http://localhost:8081/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Notebook",
    "sku": "NTB-001",
    "description": "15-inch laptop",
    "unitPrice": 3500.00,
    "initialStock": 50
  }'
```

## Event Consumption

| Event | Routing Key | Action |
|---|---|---|
| `OrderCreatedEvent` | `order.created` | Decrements stock for each item in the order (RESERVE) |
| `OrderCancelledEvent` | `order.cancelled` | Increments stock back for each item (RELEASE) |

Both handlers are **idempotent**: if the same event arrives twice, the second message is detected via the `stock_movements` table and silently discarded — stock is only updated once.

Failed messages (product not found, insufficient stock) are routed to Dead Letter Queues (`order.created.dlq`, `order.cancelled.dlq`) for analysis without blocking the consumer.

## Idempotency Design

Before reserving stock, the service checks whether a `StockMovement` record with `(orderId, RESERVE)` already exists. If it does, the event is acknowledged and skipped. The same logic applies to `RELEASE` on cancellation.

This guarantees exactly-once semantics even under RabbitMQ redelivery scenarios.

## Running Locally

**Prerequisites:** Java 17, Maven, Docker

```bash
# Start PostgreSQL (port 5436) and RabbitMQ
docker-compose up -d

# Run the service
./mvnw spring-boot:run
```

RabbitMQ Management UI: http://localhost:15672 (guest / guest)

## Running Tests

```bash
# Unit tests only (no Docker required)
./mvnw test -Dtest="ReserveStockServiceTest,ReleaseStockServiceTest"

# All tests including integration (Docker required)
./mvnw test
```

**Test coverage:**
- `ReserveStockServiceTest` — 4 unit tests: successful reserve, insufficient stock, product not found, idempotency
- `ReleaseStockServiceTest` — 3 unit tests: successful release, duplicate event skipped, invalid orderId
- `OrderEventConsumerIntegrationTest` — 4 integration tests: reserve, release, idempotency for both events
- `ProductControllerIntegrationTest` — 5 integration tests: CRUD endpoints with real PostgreSQL

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5436/inventory` | DB connection |
| `SPRING_DATASOURCE_USERNAME` | `inventory_user` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | `inventory_pass` | DB password |
| `SPRING_RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `SPRING_RABBITMQ_PORT` | `5672` | RabbitMQ port |
| `SPRING_RABBITMQ_USERNAME` | `guest` | RabbitMQ user |
| `SPRING_RABBITMQ_PASSWORD` | `guest` | RabbitMQ password |
| `PORT` | `8081` | Server port |

## Docker

```bash
# Build the image
docker build -t inventory-service .

# Run (requires PostgreSQL and RabbitMQ reachable via env vars)
docker run -p 8081:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5436/inventory \
  -e SPRING_DATASOURCE_USERNAME=inventory_user \
  -e SPRING_DATASOURCE_PASSWORD=inventory_pass \
  -e SPRING_RABBITMQ_HOST=host.docker.internal \
  inventory-service
```

## Deploy on Railway

1. Create a new project on [Railway](https://railway.app)
2. Add a **PostgreSQL** plugin — Railway will inject the connection variables
3. Point to the **same RabbitMQ** instance used by `order-service` (shared infrastructure)
4. Connect this repository — Railway detects the `Dockerfile` automatically
5. Set the following environment variables:

| Variable | Source |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` |
| `SPRING_DATASOURCE_USERNAME` | `${{Postgres.PGUSER}}` |
| `SPRING_DATASOURCE_PASSWORD` | `${{Postgres.PGPASSWORD}}` |
| `SPRING_RABBITMQ_HOST` | `${{rabbitmq.RAILWAY_PRIVATE_DOMAIN}}` |
| `SPRING_RABBITMQ_PORT` | `5672` |
| `SPRING_RABBITMQ_USERNAME` | `guest` |
| `SPRING_RABBITMQ_PASSWORD` | `guest` |

The `railway.toml` configures the health check path (`/health`) and restart policy.

## Related Services

- [order-service](https://github.com/mh001-code/order-service) — entry point, publishes `order.created` / `order.cancelled`
- [notification-service](https://github.com/mh001-code/notification-service) — consumes the same events, records notifications
