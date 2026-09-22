# Superpowers Development Workflow

This project uses the Superpowers software development methodology.

## Mandatory skill-first behavior

Before implementing a non-trivial software task, inspect the available skills
and load the skill that matches the task.

Available Superpowers skills:

- using-superpowers
- brainstorming
- writing-plans
- executing-plans
- test-driven-development
- systematic-debugging
- verification-before-completion

Skills are located under:

.agents/skills/

Use the `skill` tool to load the appropriate skill before performing the
corresponding workflow.

## Development workflow

For non-trivial feature requests:

1. Understand the user's actual requirements.
2. Inspect the existing codebase before making changes.
3. Use `brainstorming` when requirements, architecture, or design need clarification.
4. Use `writing-plans` before implementing multi-step changes.
5. Use `executing-plans` when executing an established implementation plan.
6. Use `test-driven-development` for behavior changes and bug fixes when practical.
7. Use `systematic-debugging` when diagnosing unexpected failures.
8. Use `verification-before-completion` before declaring work complete.

## Do not skip investigation

Do not immediately start editing code simply because the user requested
a feature.

First inspect:

- relevant source files
- existing tests
- project structure
- dependencies
- existing conventions
- related implementations

Prefer modifying existing patterns over introducing new patterns.

## Testing

When changing behavior:

- Prefer tests before implementation when practical.
- Run the relevant tests after implementation.
- Run type checking when applicable.
- Run linting when applicable.
- Run the project build when applicable.

Do not claim that something works without actually verifying it.

## Debugging

When something fails:

1. Reproduce the problem.
2. Gather evidence.
3. Identify the root cause.
4. Form a specific hypothesis.
5. Make the smallest appropriate fix.
6. Reproduce the original failure.
7. Verify the fix.
8. Check for regressions.

Do not repeatedly guess and patch symptoms.

## Completion

Before saying the task is complete:

- Verify the changed behavior.
- Run appropriate tests.
- Check for compilation/type errors.
- Check for obvious regressions.
- Report what was actually verified.

Never claim tests passed unless they were actually executed.

## Communication

Keep responses concise and practical.

When implementing a task, report:

- what was discovered
- what will be changed
- what was changed
- what verification was performed

Do not expose private chain-of-thought.

## Context Efficiency

- For feature work, inspect the related Controller, Service, Mapper/XML, and Thymeleaf files first.
- Read `target/`, `src/main/resources/static/vendor/`, `src/main/resources/static/fonts/`, minified JavaScript, source maps, and font files only when the user requests them or they are suspected to cause the issue.
- Prefer targeted `rg` searches by feature name, package, or filename over repository-wide exploration.
- Before editing, state the relevant files and intended change scope; do not refactor outside that scope.
- Prefer project-owned Java, XML, HTML, and JavaScript files over third-party library source files.
