@import me.yakar.tib.generate.Project
@import me.yakar.tib.pack.spring.SpringPack
@import me.yakar.tib.spec.Architecture
@param Project project
@param SpringPack pack
@param me.yakar.tib.pack.TestsModel tests
!{boolean ddd = project.architecture() == Architecture.DDD;}
!{boolean hexagonal = project.architecture() == Architecture.HEXAGONAL;}
# ${project.name()}

A Spring Boot ${pack.bootVersion()} service on Java ${project.java()}, with JPA, PostgreSQL, Flyway and Testcontainers,
scaffolded by tib. The base package is `${project.basePackage()}`.

## Build and run

The Gradle wrapper builds the service; no Gradle of its own is needed. The files of the wrapper
are Gradle's and carry no header of tib's; they are yours.

- `./gradlew build` compiles, tests and checks.@if(tests.container()) The tests start PostgreSQL in a container, so
${"  "}Docker has to run.@endif
- `./gradlew bootTestRun` starts the service against PostgreSQL in a container, through
  `Test${project.applicationClass()}`.
@if(project.docker())
- `docker build -t ${project.name()} .` builds an image of the service from its `Dockerfile`, which
${"  "}is yours. In a container, the service reaches its database through the environment, as
${"  "}`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`.
@endif
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

@if(ddd)
The service is arranged for DDD: every entity `X` is an aggregate, in three packages. `x.domain`
has `X`, `XGenerated` and `XRepository`; `x.application` has `XService`, the use cases on the
aggregate, which throw `NotFoundException` for one that is not there; `x.web` has `XController`,
`XDto` and `XMapper`, which maps the aggregate to and from its DTO. An entity refers to another by
its identifier, as `depotId`, never by the entity itself. The domain uses neither the application
nor the web, and the application does not use the web.

@endif
@if(hexagonal)
The service is arranged in the hexagonal architecture, of ports and adapters: every entity `X` has
a domain in plain Java, which knows neither JPA nor Spring, in the middle of its adapters.
`x.domain` has `X` and `XGenerated`; `x.application` has `XService`, the use cases on the domain,
which throw `NotFoundException` for one that is not there, and the ports: `x.application.port.in`
has `XUseCase`, which the inbound adapters call, and `x.application.port.out` has `XStore`, which
an outbound adapter implements. `x.adapter.in.web` has `XController`, `XDto` and `XWebMapper`,
which maps the domain to and from its DTO; `x.adapter.out.persistence` has `XEntity`, the table as
JPA maps it, `XJpaRepository`, `XPersistenceMapper`, which maps the domain to and from `XEntity`,
and `XPersistenceAdapter`, the `XStore` on the table. An entity refers to another by its
identifier, as `depotId`, never by the entity itself. The application does not use the adapters,
and an inbound adapter does not use an outbound one, nor the reverse.

@endif
For an entity `X`, its code goes in `X`, never in `XGenerated`: @if(hexagonal)`XGenerated`, `XEntity`, `XDto`,
`XWebMapper` and `XPersistenceMapper` are tib's; `X`, `XUseCase`, `XStore`, `XService`,
`XController`, `XJpaRepository`, `XPersistenceAdapter`@else`XGenerated`, `XDto` and `XMapper`
are tib's; `X`, `XRepository`, `XService`, `XController`@endif and the migration are yours@if(tests.any()), and so are
their tests: ${tests.entityTests()}. Their sample entities and DTOs come
from `XSamples`, which is tib's and follows the spec: a test changes a value through the builder,
as in `XSamples.x().name("Other").build()`.@else.@endif
`X` keeps a constructor without parameters: the files of tib's make an `X` and set its values.
@if(ddd || hexagonal)
`ConflictHandler`, in the base package, answers a request that breaks a constraint of the database
with 409 Conflict, and a request for an entity that is not there, which a service throws as
`NotFoundException`, with 404 Not Found; both are yours.
@else
`ConflictHandler`, in the base package, answers a request that breaks a constraint of the database
with 409 Conflict; it is yours.
@endif
@if(tests.archunit())
`ArchitectureTests`, in the tests of the base package, checks the rules of the architecture on the
@if(ddd)
classes of the service with ArchUnit: the domain of an entity uses neither its application nor its
web, the application does not use the web, the domain of an aggregate does not use the domain of
another, and there is no cycle between the packages of the entities; it is yours.
@elseif(hexagonal)
classes of the service with ArchUnit: the domain knows neither JPA nor Spring, the application does
not use the adapters, an inbound adapter does not use an outbound one, nor the reverse, and there
is no cycle between the packages of the entities; it is yours.
@else
classes of the service with ArchUnit: the controller of an entity over its service over its
repository, and no cycle between the packages of the entities; it is yours.
@endif
@endif<%-- keeps the blank line below --%>

The Java that tib writes is in the layout of google-java-format ${pack.formatterVersion()}: formatted
with it, the files of tib's stay as tib wrote them.

`tib status` lists the files of tib's that differ from what it wrote, and the spec files and DDL
that changed since it read them, and exits with 1 when one does. tib writes only into a service
whose files that git tracks are committed: commit first, or pass `--allow-dirty`.
