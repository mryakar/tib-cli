<!-- Written by tib when the service was created; it is yours from then on. -->
# vehicle-service

A Spring Boot 4.1.2 service on Java 21, with JPA, PostgreSQL, Flyway and Testcontainers,
scaffolded by tib. The base package is `com.example.vehicleservice`.

## Build and run

The Gradle wrapper builds the service; no Gradle of its own is needed. The files of the wrapper
are Gradle's and carry no header of tib's; they are yours.

- `./gradlew build` compiles, tests and checks. The tests start PostgreSQL in a container, so
  Docker has to run.
- `./gradlew bootTestRun` starts the service against PostgreSQL in a container, through
  `TestVehicleServiceApplication`.
- `./gradlew bootRun` starts the service against the database of
  `src/main/resources/application.yaml`.

## What tib owns

tib keeps what it generated in `.tib/state.yaml`: every file with its owner and the hash of its
content. The first line of each generated file, but for the Gradle wrapper, says whose it is:

- *Written by tib when the service was created*, *with the first entity of the service* or *when
  the entity was created*: the file is yours; tib never writes it again. A change of the schema
  goes in a new migration.
- *Written by tib on every run*: tib writes the file again on every run; do not edit it.

The entities come from a spec. To add or change one, change the spec and run
`tib generate entity --spec <file>`, or for tables in SQL, `tib generate entity --ddl <file>`;
with `--dry-run`, tib shows what it would change and changes nothing. To remove an entity, take
it out of the spec and run `tib generate entity --spec <file>`: tib deletes its files, but for
those you edited and the migration; dropping its table is a new migration.

For an entity `X`, its code goes in `X`, never in `XGenerated`: `XGenerated`, `XDto` and `XMapper`
are tib's; `X`, `XRepository`, `XService`, `XController` and the migration are yours, and so are
their tests: `XRepositoryTests` and `XApiTests` against PostgreSQL in a container, and
`XServiceTests` with the repository and the mapper mocked. Their sample entities and DTOs come
from `XSamples`, which is tib's and follows the spec: a test changes a value through the builder,
as in `XSamples.x().name("Other").build()`. `ConflictHandler`, in the base package, answers a
request that breaks a constraint of the database with 409 Conflict; it is yours.

The Java that tib writes is in the layout of google-java-format 1.36.1: formatted
with it, the files of tib's stay as tib wrote them.

`tib status` lists the files of tib's that differ from what it wrote, and the spec files and DDL
that changed since it read them, and exits with 1 when one does. tib writes only into a service
whose files that git tracks are committed: commit first, or pass `--allow-dirty`.
