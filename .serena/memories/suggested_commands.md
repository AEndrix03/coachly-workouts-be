# Suggested Commands (Windows)
## Development
- `.\\mvnw.cmd spring-boot:run` : run service locally.
- `.\\mvnw.cmd test` : run tests.
- `.\\mvnw.cmd clean verify` : full build checks.
- `.\\mvnw.cmd clean package` : build JAR in `target/`.

## Optional Docker
- `docker build -t coachly-workouts-be:latest .`
- `docker run --rm -p 8090:8090 -e COACHLY_DB_URL=... -e COACHLY_DB_USERNAME=... -e COACHLY_DB_PASSWORD=... coachly-workouts-be:latest`

## Utility (PowerShell)
- `Get-ChildItem` (list files)
- `Get-Content <file>` (read file)
- `Select-String -Path <path> -Pattern <pattern>` (search text)
- `git status`, `git add`, `git commit`