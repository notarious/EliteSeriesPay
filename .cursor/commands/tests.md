Add or improve tests for the selected change.

Focus on:

- service-level business rules;
- payment calculations;
- billing calculations;
- ACTIVE versus VOIDED behavior;
- validation edge cases;
- repository-level filtering and pagination.

Rules:

- Prefer focused unit tests for calculation logic.
- Use repository tests only when persistence/query behavior matters.
- Do not change production code unless required to make the tests meaningful.
- Do not weaken existing assertions.