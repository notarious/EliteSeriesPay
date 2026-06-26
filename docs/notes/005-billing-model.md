# Billing Model

## Decision

Project memberships support multiple billing modes.

Current modes:

- SUBSCRIPTION
- PACKAGE

## Reason

Different communities charge participants differently.

The billing model should be extensible without changing the payment model.

## Consequences

MembershipBillingCalculator determines payment status.

Billing information belongs to ProjectMembership rather than Participant.

Payment records remain independent from billing calculations.