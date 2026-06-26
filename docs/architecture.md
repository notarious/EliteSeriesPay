# Architecture

## Overview

EliteSeriesPay is a local-first Spring Boot application for managing projects, participants, payments, and community budgets.

The application follows a classic layered architecture and keeps business logic independent from the web layer.

```
Browser (Thymeleaf)
        │
Controllers
        │
Services
        │
Repositories
        │
SQLite
```

## Technology Stack

- Java 21
- Spring Boot
- Spring Data JPA
- SQLite
- Flyway
- Thymeleaf
- Bootstrap
- JUnit

## Layers

### Controller

Responsibilities:

- HTTP endpoints
- Request mapping
- Form binding
- Redirects
- View rendering

Controllers must not contain business logic.

### Service

Responsibilities:

- Business rules
- Validation of business invariants
- Transactions
- Coordination between repositories

### Repository

Responsibilities:

- Database access only
- Query methods
- Pagination and filtering

Repositories must not contain business logic.

## Domain Model

The main entities are:

- Project
- Participant
- ProjectMembership
- Payment
- ApplicationSettings

Participants are global entities.

Projects are linked through ProjectMembership.

Payments belong to both a participant and a project.

## Design Principles

- Layered architecture
- Thin controllers
- Business logic in services
- Repository-only persistence
- Immutable financial history
- Flyway manages all schema changes
- SQLite is used as an embedded database
- Local-first application without external infrastructure

## Testing

Business rules are tested at the service level.

Repository tests verify persistence behavior.

Controllers remain intentionally thin.