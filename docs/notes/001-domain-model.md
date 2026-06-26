# Domain Model

## Decision

Participants are global entities.

Projects are connected through ProjectMembership rather than embedding participants directly inside projects.

## Reason

A participant may support multiple projects simultaneously.

Membership contains project-specific information that should not belong to the participant itself.

## Consequences

- Participants can be reused across projects.
- Membership stores status and billing information.
- Participant history remains available independently of project membership.