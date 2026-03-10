# Project Overview
- Name: coachly-workouts-be
- Purpose: Spring Boot backend microservice for Coachly workouts domain.
- Language/stack: Java 25, Spring Boot 4.1.0-M2, Spring Data JPA, PostgreSQL, Lombok, MapStruct, Maven Wrapper.
- Packaging: single-module Maven project (`workouts-be`).
- Runtime config in `src/main/resources/application.yaml` (server port 8090, DB env vars `COACHLY_DB_URL`, `COACHLY_DB_USERNAME`, `COACHLY_DB_PASSWORD`).
- Main package: `it.aredegalli.coachly.workout` with layered subpackages (`controller`, `service`, `repository`, `model`, `mapper`).
- Tests package aligns with main package under `src/test/java`.
- Additional architecture docs in `docs/architecture/`.