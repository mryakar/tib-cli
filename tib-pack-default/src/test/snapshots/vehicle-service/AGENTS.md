<!-- Written by tib when the service was created; it is yours from then on. -->
# vehicle-service

A Spring Boot 3.5.16 service on Java 21, with JPA, PostgreSQL, Flyway and Testcontainers,
scaffolded by tib. The base package is `com.example.vehicleservice`.

## Build and run

With Gradle 9 installed, run `gradle wrapper` once; from then on the wrapper builds the service.

- `./gradlew build` compiles, tests and checks. The tests start PostgreSQL in a container, so
  Docker has to run.
- `./gradlew bootTestRun` starts the service against PostgreSQL in a container, through
  `TestVehicleServiceApplication`.
- `./gradlew bootRun` starts the service against the database of
  `src/main/resources/application.yaml`.

## What tib owns

tib keeps what it generated in `.tib/state.yaml`: every file with its owner and the hash of its
content. The first line of each generated file says whose it is:

- *Written by tib when the service was created* or *when the entity was created*: the file is
  yours; tib never writes it again. A change of the schema goes in a new migration.
- *Written by tib on every run*: tib writes the file again on every run; do not edit it.

The entities come from a spec. To add or change one, change the spec and run
`tib generate entity --spec <file>`, or for tables in SQL, `tib generate entity --ddl <file>`;
with `--dry-run`, tib shows what it would change and changes nothing.

For an entity `X`, its code goes in `X`, never in `XGenerated`: `XGenerated`, `XDto` and `XMapper`
are tib's; `X`, `XRepository`, `XService`, `XController` and the migration are yours, and so are
their tests: `XRepositoryTests` and `XApiTests` against PostgreSQL in a container, and
`XServiceTests` with the repository and the mapper mocked. Keep `XGenerated`, `XDto` and `XMapper`
out of your formatter: tib writes them again in its own layout.

`tib status` lists the files of tib's that differ from what it wrote, and exits with 1 when one
does. tib writes only into a service whose files that git tracks are committed: commit first, or
pass `--allow-dirty`.
