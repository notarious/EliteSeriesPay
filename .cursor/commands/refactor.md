Refactor the selected code without changing behavior.

Goals:

- reduce duplication;
- improve naming;
- simplify control flow;
- keep public behavior unchanged;
- preserve existing validation;
- preserve existing tests;
- preserve financial history rules.

Rules:

- Do not introduce new frameworks.
- Do not move business logic into controllers.
- Do not remove tests.
- Do not change database schema unless explicitly requested.

After refactoring, explain what changed and why.