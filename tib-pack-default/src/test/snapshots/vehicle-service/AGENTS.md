<!-- Written by tib on every run. Do not edit: your changes would be lost. -->
# vehicle-service

A Spring Boot 3.5.16 service on Java 21, with JPA, PostgreSQL,
Flyway and Testcontainers, scaffolded by tib. The base package is `com.example.vehicleservice`.

## Build and test

- `gradle build` compiles, tests and checks. The tests start PostgreSQL in a container, so Docker
  has to run.
- `gradle bootRun` starts the service against the database of `src/main/resources/application.yaml`.

## What tib owns

tib keeps what it generated in `.tib/state.yaml`: every file with its owner and the hash of its
content. Each generated file says in its first line which kind it is:

- *Written once*: the file is yours from then on; tib never touches it again.
- *Written on every run*: do not edit it; change the spec and run tib again.
- *Edited in place*: tib adds to it and leaves the rest alone.

Run `tib` with `--dry-run` to see what it would change before it changes anything.
