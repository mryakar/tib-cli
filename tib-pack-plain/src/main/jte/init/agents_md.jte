@import me.yakar.tib.generate.Project
@import me.yakar.tib.pack.plain.PlainPack
@import me.yakar.tib.spec.Architecture
@param Project project
@param PlainPack pack
@param me.yakar.tib.pack.TestsModel tests
!{String database = project.name().replace('-', '_');}
!{boolean ddd = project.architecture() == Architecture.DDD;}
!{boolean hexagonal = project.architecture() == Architecture.HEXAGONAL;}
# ${project.name()}

A service in plain Java ${project.java()}, scaffolded by tib, without a dependency injection framework:
its objects are wired by hand. It answers HTTP with the server of the JDK, keeps its entities in
PostgreSQL with JDBC, migrates its schema with Flyway and reads and writes JSON with Jackson; its
tests start PostgreSQL in a container with Testcontainers. The base package is
`${project.basePackage()}`.

## Build and run

The Gradle wrapper builds the service; no Gradle of its own is needed. The files of the wrapper
are Gradle's and carry no header of tib's; they are yours.

- `./gradlew build` compiles, tests and checks.@if(tests.container()) The tests start PostgreSQL in a container, so
${"  "}Docker has to run.@endif
- `./gradlew testRun` starts the service against PostgreSQL in a container, through
  `Test${project.applicationClass()}`.
@if(project.docker())
- `docker build -t ${project.name()} .` builds an image of the service from its `Dockerfile`, which
${"  "}is yours. In a container, the service reaches its database through the environment variables
${"  "}below.
@endif
- `./gradlew run` starts the service against the database that the environment names; `Config`
  reads these variables, each with its default:
  - `DATABASE_URL`, the JDBC URL of the database: `jdbc:postgresql://localhost:5432/${database}`
  - `DATABASE_USER`, its user: `${database}`
  - `DATABASE_PASSWORD`, the password of the user: `${database}`
  - `PORT`, the port of the API: `8080`

## How it is wired

`${project.applicationClass()}` reads `Config`, migrates the database with the migrations under
`src/main/resources/db/migration`, and starts the HTTP server of the JDK with the routes that
`Routes` adds: for every entity `X` with an API, the handler that `XWiring` makes, at `/api/` and
the plural of `x`, and one for every other path, which is not found, all behind `ErrorHandler`.
`Routes` is tib's and follows the spec, so that a new entity needs no change of
`${project.applicationClass()}`. `XWiring`, in the package `x`, is yours: it makes the objects of
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

@if(ddd)
The service is arranged for DDD: every entity `X` is an aggregate, in three packages. `x.domain`
has `X`, `XGenerated` and `XRepository`; `x.application` has `XService`, the use cases on the
aggregate, which throw `NotFoundException` for one that is not there; `x.web` has `XHandler`,
`XDto` and `XMapper`, which maps the aggregate to and from its DTO. An entity refers to another by
its identifier, as `depotId`: there is no ORM. The domain uses neither the application nor the
web, and the application does not use the web. Its code goes in `X`, never in `XGenerated`:
@elseif(hexagonal)
The service is arranged in the hexagonal architecture, of ports and adapters: every entity `X` has
a domain in plain Java, which knows neither JDBC nor the HTTP server, in the middle of its adapters.
`x.domain` has `X` and `XGenerated`; `x.application` has `XService`, the use cases on the domain,
which throw `NotFoundException` for one that is not there, and the ports: `x.application.port.in`
has `XUseCase`, which the inbound adapters call, and `x.application.port.out` has `XStore`, which
an outbound adapter implements. `x.adapter.in.web` has `XHandler`, `XDto` and `XWebMapper`, which
maps the domain to and from its DTO; `x.adapter.out.persistence` has `XRepository` and
`XPersistenceAdapter`, the `XStore` on it. An entity refers to another by its identifier, as
`depotId`: there is no ORM. The application does not use the adapters, and an inbound adapter does
not use an outbound one, nor the reverse. Its code goes in `X`, never in `XGenerated`:
@else
Every class of an entity `X` is in the package of the entity, `x`, and refers to another entity by
its identifier, as `depotId`: there is no ORM. Its code goes in `X`, never in `XGenerated`:
@endif
`XGenerated`, `XRepository`, the SQL of its table, `XDto`, which checks the rules of the spec, and
@if(hexagonal)`XWebMapper` are tib's; `X`, `XUseCase`, `XStore`, `XService`, `XHandler`,
`XPersistenceAdapter`, `XWiring`@else`XMapper` are tib's; `X`, `XService`, `XHandler`, `XWiring`@endif and the migration are yours@if(tests.any()), and so are
their tests: ${tests.entityTests()}. Their sample entities and DTOs come
from `XSamples`, which is tib's and follows the spec: a test changes a value through the builder,
as in `XSamples.x().name("Other").build()`.@else.@endif
`X` keeps a constructor without parameters: the files of tib's make an `X` and set its values.
@if(hexagonal)
A query of your own goes in `XPersistenceAdapter`, which takes the connection of the unit of work
from `Transactions.connection()`.
@else
A query of your own goes in a class of yours, which takes the connection of the unit of work from
`Transactions.connection()`.
@endif
`ErrorHandler`, in the base package, answers a request for an entity that is not there with 404
Not Found, one that breaks a constraint of the database or whose value does not fit its column
with 409 Conflict, and one whose DTO breaks a rule or cannot be read with 400 Bad Request, as
problem details; it is yours, and so are `NotFoundException`, `BadRequestException`, `Http` and
`Page`.
@if(tests.archunit())
`ArchitectureTests`, in the tests of the base package, checks the rules of the architecture on the
@if(ddd)
classes of the service with ArchUnit, by their packages: the domain of an entity uses neither its
application nor its web, the application does not use the web, the domain of an aggregate does not
use the domain of another, and there is no cycle between the packages of the entities; it is yours.
@elseif(hexagonal)
classes of the service with ArchUnit, by their packages: the domain knows neither JDBC nor the HTTP
server, the application does not use the adapters, an inbound adapter does not use an outbound
one, nor the reverse, and there is no cycle between the packages of the entities; it is yours.
@else
classes of the service with ArchUnit, by their names and packages: the handler of an entity over
its service over its repository, a handler used by the wiring alone, and no cycle
between the packages of the entities; it is yours.
@endif
@endif<%-- keeps the blank line below --%>

The Java that tib writes is in the layout of google-java-format ${pack.formatterVersion()}: formatted
with it, the files of tib's stay as tib wrote them.

`tib status` lists the files of tib's that differ from what it wrote, and the spec files and DDL
that changed since it read them, and exits with 1 when one does. tib writes only into a service
whose files that git tracks are committed: commit first, or pass `--allow-dirty`.
