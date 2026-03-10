# DTO Design (Workout Domain)

## Obiettivo
I DTO in `src/main/java/it/aredegalli/coachly/workout/dto` rappresentano il payload applicativo delle entita JPA evitando campi tecnici o di persistenza.

## Regole applicate
- Campi tecnici esclusi: `createdAt`, `updatedAt`, `deletedAt`.
- Nessun riferimento JPA nei DTO (`@ManyToOne`, `@OneToMany`), solo valori semplici o strutture annidate.
- Le relazioni gerarchiche espongono chiavi esplicite (`workoutId`, `blockId`, `entryId`, `exerciseId`) invece di riferimenti entity.
- Tutti i DTO applicativi sono classi Lombok (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`) per uso pratico in service/controller.
- Gli enum di dominio usano i tipi condivisi del `workouts-be-lib`: `WorkoutStatus`, `SessionStatus`, `SetType`, `LoadUnit`.

## Mappatura sintetica
- `WorkoutDto`: dati funzionali del workout template e lista ordinata dei blocchi.
- `WorkoutBlockDto`: blocco ordinato con `entries`.
- `WorkoutBlockEntryDto`: slot esercizio del blocco con `exerciseId` e `sets`.
- `WorkoutSetDto`: singola serie pianificata del template.
- `WorkoutSessionDto`: dati di sync/statistica della sessione; il campo `snapshot` usa `it.aredegalli.coachly.workout.dto.snapshot.SessionSnapshotDto`.

## Nota operativa
Con MapStruct nel `pom.xml`, i mapper Entity <-> DTO devono ricostruire anche le relazioni padre/figlio dell'aggregato `Workout`, cosi da poter usare `CascadeType.ALL` senza leak di dettagli JPA all'esterno.

## Build note: MapStruct + Lombok
- La compilazione usa `maven-compiler-plugin` con annotation processors espliciti.
- Ordine raccomandato: `lombok`, `lombok-mapstruct-binding`, `mapstruct-processor`.
- Il binding evita errori MapStruct su getter/setter/costruttori generati da Lombok durante l'annotation processing.
- La serializzazione/deserializzazione del payload `snapshot` JSONB va centralizzata nel mapper di sessione e deve usare i DTO condivisi del `workouts-be-lib`.
