# Payment History

## Decision

Payments are treated as financial records.

Financial history is preserved instead of being physically removed.

## Reason

Removing payments would make historical reports unreliable.

Maintaining an audit trail is more important than simplifying deletion.

## Consequences

- Payments can become VOIDED.
- ACTIVE payments participate in calculations.
- VOIDED payments remain visible as historical records.