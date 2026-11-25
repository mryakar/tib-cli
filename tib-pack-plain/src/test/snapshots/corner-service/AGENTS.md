<!-- Written by tib when the service was created; it is yours from then on. -->
# corner-service

A service in plain Java 25, scaffolded by tib, without a framework of dependency injection:
its objects are wired by hand. It answers HTTP with the server of the JDK, keeps its entities in
PostgreSQL with JDBC, migrates its schema with Flyway and reads and writes JSON with Jackson; its
tests start PostgreSQL in a container with Testcontainers. The base package is
`me.yakar.cornerservice`.

## Build and run

The Gradle wrapper builds the service; no Gradle of its own is needed. The files of the wrapper
are Gradle's and carry no header of tib's; they are yours.

- `./gradlew build` compiles, tests and checks. The tests start PostgreSQL in a container, so
  Docker has to run.
- `./gradlew testRun` starts the service against PostgreSQL in a container, through
  `TestCornerServiceApplication`.
- `docker build -t corner-service .` builds an image of the service from its `Dockerfile`, which
  is yours. In a container, the service reaches its database through the environment variables
  below.
- `./gradlew run` starts the service against the database that the environment names; `Config`
  reads these variables, each with its default:
  - `DATABASE_URL`, the JDBC URL of the database: `jdbc:postgresql://localhost:5432/corner_service`
  - `DATABASE_USER`, its user: `corner_service`
  - `DATABASE_PASSWORD`, the password of the user: `corner_service`
  - `PORT`, the port of the API: `8080`

## How it is wired

`CornerServiceApplication` reads `Config`, migrates the database with the migrations under
`src/main/resources/db/migration`, and starts the HTTP server of the JDK with the routes that
`Routes` adds: for every entity `X` with an API, the handler that `XWiring` makes, at `/api/` and
the plural of `x`, and one for every other path, which is not found, all behind `ErrorHandler`.
`Routes` is tib's and follows the spec, so that a new entity needs no change of
`CornerServiceApplication`. `XWiring`, in the package `x`, is yours: it makes the objects of
the entity by hand, so what a class of the entity takes in its constructor, it is given there.
`Transactions` runs a unit of work on one connection, in one transaction, which the repositories of
the work take from it; a service runs each of its use cases so. `Json` holds the one mapper of the
API.

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

Every class of an entity `X` is in the package of the entity, `x`, and refers to another entity by
its identifier, as `depotId`: there is no ORM. Its code goes in `X`, never in `XGenerated`:
`XGenerated`, `XRepository`, the SQL of its table, `XDto`, which checks the rules of the spec, and
`XMapper` are tib's; `X`, `XService`, `XHandler`, `XWiring` and the migration are yours, and so are
their tests: `XRepositoryTests` and `XApiTests` against PostgreSQL in a container, and
`XServiceTests` with the repository and the mapper mocked. Their sample entities and DTOs come
from `XSamples`, which is tib's and follows the spec: a test changes a value through the builder,
as in `XSamples.x().name("Other").build()`. `ErrorHandler`, in the base package, answers a
request for an entity that is not there with 404 Not Found, one that breaks a constraint of the
database with 409 Conflict, and one whose DTO breaks a rule or cannot be read with 400 Bad
Request, as problem details; it is yours, and so are `NotFoundException`, `BadRequestException`,
`Http` and `Page`.
`ArchitectureTests`, in the tests of the base package, checks the rules of the architecture on the
classes of the service with ArchUnit, by their names and packages: the handler of an entity over
its service over its repository, a handler used by the wiring of its entity alone, and no cycle
between the packages of the entities; it is yours.

The Java that tib writes is in the layout of google-java-format 1.36.1: formatted
with it, the files of tib's stay as tib wrote them.

`tib status` lists the files of tib's that differ from what it wrote, and the spec files and DDL
that changed since it read them, and exits with 1 when one does. tib writes only into a service
whose files that git tracks are committed: commit first, or pass `--allow-dirty`.
