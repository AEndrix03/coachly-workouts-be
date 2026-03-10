# Repository Guidelines

## Project Structure & Module Organization
This service is a single-module Spring Boot backend for workouts.

- `src/main/java/it/aredegalli/coachly/workout/`: application code (`controller`, `service`, `repository`, `model`, `mapper`, `dto`).
- `src/main/resources/`: runtime configuration (`application.yaml`).
- `src/test/java/it/aredegalli/coachly/workout/`: tests.
- `docs/architecture/` and `docs/flows/`: technical design and workflow notes.
- `.serena/project.yml` and `.serena/memories/`: SerenaMCP project metadata and onboarding memory.
- `schema.sql`: PostgreSQL schema and enums (shared catalog/domain references).

Keep production/test package structures aligned.

## Build, Test, and Development Commands
Use Maven Wrapper from repo root:

- `.\\mvnw.cmd spring-boot:run`: run locally (Windows).
- `.\\mvnw.cmd test`: run tests.
- `.\\mvnw.cmd clean verify`: full build checks.
- `.\\mvnw.cmd clean package`: create JAR in `target/`.

## Coding Style & Naming Conventions
- Java 25, Spring Boot conventions, layered architecture.
- Class names `PascalCase`, methods/fields `camelCase`, constants and enum values `UPPER_SNAKE_CASE`.
- Avoid Java keywords as identifiers (`static`, `public`, `private`, etc.).
- Preserve existing formatting style in touched files.

## Testing Guidelines
- Framework: JUnit 5 (`spring-boot-starter-test`).
- Naming: `*Test` / `*Tests`.
- Add/update tests for every behavior change.

## Commit & Pull Request Guidelines
- Use Conventional Commits (e.g. `feat(workout): add mapper for workout aggregate`).
- Keep commits scoped and atomic.
- PR must include: summary, rationale, test evidence, and schema/config impact.

## Agent Workflow (Mandatory)
- Always use SerenaMCP as primary workflow:
  - activate project and verify onboarding state at session start;
  - use Serena tools for discovery/navigation before edits.
- Treat `.serena/project.yml` as the Serena project descriptor for this repository.
- Always read relevant files under `docs/` before implementation.
- Always update `docs/` when architecture, mapping, contracts, or workflows change.
- Always create a commit at the end of each requested implementation step unless explicitly told not to.
- Keep working tree clean after each commit.
