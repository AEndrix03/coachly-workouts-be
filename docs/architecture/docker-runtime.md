# Docker Runtime

## Obiettivo
Esecuzione production-like con build completa Maven e runtime leggero su porta `8080`.

## Scelte implementative
- Build multi-stage:
  - stage `build` con `eclipse-temurin:25-jdk` e `./mvnw clean package`.
  - stage runtime con `eclipse-temurin:25-jre`.
- Processo non-root (`spring`, uid `10001`).
- Porta container esposta: `8080`.
- Virtual threads abilitati:
  - `spring.threads.virtual.enabled=true` in `application.yaml`.
  - `SPRING_THREADS_VIRTUAL_ENABLED=true` + `-Dspring.threads.virtual.enabled=...` in entrypoint.

## Comandi
Build immagine:
```bash
docker build -t coachly-workouts-be:latest .
```

Run locale:
```bash
docker run --rm -p 8080:8080 \
  -e COACHLY_DB_URL=jdbc:postgresql://host.docker.internal:5432/coachly \
  -e COACHLY_DB_USERNAME=postgres \
  -e COACHLY_DB_PASSWORD=postgres \
  coachly-workouts-be:latest
```
