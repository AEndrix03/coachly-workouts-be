# Style and Conventions
- Follow Spring Boot layered architecture and existing package structure.
- Naming: classes PascalCase, methods/fields camelCase, constants/enums UPPER_SNAKE_CASE.
- Keep formatting/style consistent with touched files.
- Avoid Java keywords as identifiers.
- Use Dto/entity mapping via MapStruct where applicable and keep persistence details out of external contracts.
- Preserve docs alignment: when contracts/architecture/workflows change, update `docs/` accordingly.