# Validation

## Decision

Validation is divided into two layers.

## User Input

Bean Validation validates form input.

Examples:

- required fields
- length
- format

## Business Rules

Services validate business invariants.

Examples:

- duplicate memberships
- invalid project operations
- payment consistency

## Consequences

Controllers remain simple.

Business logic does not depend on the web layer.

The service layer can be reused independently from the UI.