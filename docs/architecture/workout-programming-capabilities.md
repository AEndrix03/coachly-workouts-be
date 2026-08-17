# Workout programming capability extension

The mobile Workout Detail redesign consumes a structured, backward-compatible
workout aggregate.

## Existing aggregate

`Workout -> WorkoutBlock -> WorkoutBlockEntry -> WorkoutSet` already provides
stable UUIDs, ordering, exercise references, set types, reps, load, rest and
notes. Historically, a block with multiple entries implicitly means superset.

## Contract gaps

The current contract cannot faithfully persist sections, circuit semantics,
rounds, repetition ranges or mutually exclusive intensity targets (RIR, RPE,
percentage of 1RM). These capabilities must not be simulated only in the UI.

The extension therefore follows these compatibility rules:

- all new fields are nullable and legacy payloads keep their current meaning;
- a missing group type is inferred as `superset` only for multi-entry blocks;
- workouts without explicit section metadata form one implicit section;
- intensity is represented by one discriminator and its matching values;
- set performance remains outside this template API;
- advanced programming metadata is prescription data, not progression advice.

Database changes are additive and idempotent. They must be applied before a
service build that validates the extended JPA model.
