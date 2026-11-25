# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- A service is on Java 21 or 25: `tib init service NAME --java 25`, or `java: 25` in its spec. The
  default is 21, as before; the build of the service compiles for its Java, and `AGENTS.md` names
  it. The Java is chosen when the service is created: `tib generate entity --spec` refuses a spec
  on another Java than the one of the service.
- A service has the kinds of tests it chooses: `unit`, `integration` and `api`, as
  `tib init service NAME --tests unit,api` or `tests: [unit, api]` in its spec. All three are the
  default, as before. `unit` writes the tests of the services of the entities, `integration` the
  tests of their repositories and the test of the application, and `api` the tests of their API;
  a service with only unit tests builds without Docker. The kinds are chosen when the service is
  created, as its Java is.
- A service may have a `Dockerfile`: `tib init service NAME --docker`, or `docker: true` in its
  spec. tib writes it once, with a `.dockerignore`; it builds the service with its Gradle wrapper,
  extracts the layers of its jar as Spring Boot does, and runs it on the Java runtime of the
  service, as a user without root rights. Without the option, there is none, as before.
- `tib init service` without a name and without `--spec` asks for the name, the Java version, the
  architecture, the kinds of tests and the Dockerfile, on standard error, and reads the answers from standard input;
  an empty answer takes the default, and one that tib cannot use is asked again. When the answers
  end first, it exits with 2, as it did for a command line without the name.
- A service has an architecture: `--architecture` or `architecture:` in its spec, chosen when the
  service is created. `anemic`, the default, arranges the code as before. `ddd` makes every entity
  an aggregate in three packages: its `domain`, with the entity and its repository; its
  `application`, with the service of its use cases on the aggregate; and its `web`, with its
  controller, DTO and mapper. `hexagonal` puts the domain of every entity, in plain Java without
  JPA or Spring, among ports and adapters: its `domain`; its `application`, with the service of its
  use cases, `XUseCase`, the port that the inbound adapters call, and `XStore`, the port that keeps
  the domain; its `adapter.in.web`, with its controller, DTO and mapper; and its
  `adapter.out.persistence`, with the JPA entity of its table, its repository, the mapper between
  the two and the adapter of the store. In both, an entity refers to another by its identifier,
  not by the entity, and a service throws `NotFoundException`, which `ConflictHandler` answers
  with 404.
- A fourth kind of tests, `archunit`: `ArchitectureTests`, written once, checks the rules of the
  architecture on the classes of the service with ArchUnit. For the anemic architecture: the
  controller of an entity over its service over its repository. For DDD: the domain uses neither
  the application nor the web, the application does not use the web, and the domain of an
  aggregate does not use the domain of another. For the hexagonal architecture: the domain knows
  neither JPA nor Spring, the application does not use the adapters, and an inbound adapter does
  not use an outbound one, nor the reverse. For all three, no cycle between the packages of the
  entities. It is not among the kinds that a service has by default.

- A second template pack, `plain`: `tib init service NAME --pack plain`, `pack: plain` in the spec,
  or the answer to a new question of `tib init service`, which comes before the architecture. It
  generates a service in plain Java, without a framework of dependency injection: its application
  class reads its configuration from the environment (`DATABASE_URL`, `DATABASE_USER`,
  `DATABASE_PASSWORD` and `PORT`, each with a default), migrates the database with Flyway and
  starts the HTTP server of the JDK with the routes of the entities, which `Routes`, written on
  every run, wires by hand. An entity gets plain Java values, a repository in SQL on JDBC, a DTO
  that checks the rules of the spec itself, a mapper, a service whose use cases run in transactions,
  and a handler with the API of the default pack, its JSON and pages included; errors are answered
  as problem details with 404, 409 and 400. Its tests are of the same four kinds, with JUnit,
  Mockito, Testcontainers, the HTTP client of the JDK and ArchUnit, and a `Dockerfile` runs what
  `installDist` installs. The pack arranges a service in all three architectures. In DDD, every
  entity is an aggregate in its `domain`, with the entity and its repository, its `application`,
  with the service of its use cases on the aggregate, and its `web`, with its handler, DTO and
  mapper. In the hexagonal architecture, the domain of every entity knows neither JDBC nor the
  HTTP server: its `application` has the service of its use cases and the ports `XUseCase` and
  `XStore`; its `adapter.in.web` has its handler, DTO and mapper; and its
  `adapter.out.persistence` has its repository in SQL and `XPersistenceAdapter`, the store on it,
  whose tests run against PostgreSQL. The rules of `ArchitectureTests` are the ones of the
  architecture, by the packages of the classes. A service stays with its pack, as before:
  `tib generate entity` refuses a spec of the other one.

### Changed

- `tib init service` writes the wrapper of Gradle 9.9.1 into a new service, instead of 9.8.2.

## [0.4.0] - 2026-11-19

### Added

- `tib generate entity --ddl` reads the schema that `pg_dump --schema-only` writes, as it writes
  it: the commands of psql in it, such as the `\restrict` of PostgreSQL 17.6 and 18, the owners and
  the defaults that `ALTER TABLE` sets, and the functions, triggers and views of the database, which
  tib skips; an identity that `ALTER TABLE` adds to a column makes the identifier an identity. The
  statements other than `CREATE TABLE` and `ALTER TABLE` are no longer parsed, so SQL that tib
  cannot parse in them is no longer a problem; one that tib cannot parse in a table is named with
  its line and column in the DDL, each of them.
- `--ddl` reads a dump with data too: the rows after `COPY ... FROM stdin` are skipped up to the
  line `\.`, and the keys that `pg_dump` writes after them are read; rows that no such line ends
  are a problem. Row level security, a replica identity and the statistics of a column that
  `ALTER TABLE` sets are skipped, as the owner is; an `ALTER TABLE` that changes anything else, as
  for a partition, is still a problem.
- The identifier of an entity in the spec may name its column, as `id: { column: ... }`, as a
  field and a relation do; without one, its column is the field in upper snake case, as before.
- A field of the spec may be of the type `Text`: a `String` without a limit, which the default
  pack writes as a `TEXT` column, without a limit in the validation of the DTO.

### Changed

- Breaking: a primary key of DDL whose default is the next value of a sequence, `nextval(...)`, as
  `pg_dump` writes a `SERIAL`, is an identity, as a `SERIAL` is: the database assigns its values.
  It was an identifier with a sequence of tib's.
- Breaking: a `TEXT`, `CLOB` or `NCLOB` column of DDL is a `Text`; it was a `String` of 255
  characters, into which a longer value of the table did not fit. In a service that an earlier tib
  read from such DDL, the next run takes the limit out of the DTO and says that the migration
  would differ now: the column that tib's migration made is a `VARCHAR(255)`, and widening it is a
  migration of yours.
- The DDL keeps the name of the column of the primary key, as `city_id`, which tib wrote as
  `CITY_ID` before. In a service that an earlier tib read from DDL in lower case, the next run
  writes `XGenerated` with that name and says that the migration would differ now; PostgreSQL
  takes the two names for one, so no migration is needed.
- A reserved word as the column of an identifier is reported at `id.column`, as in
  `entities[0].id.column: 'USER' is a reserved word in SQL; give the identifier a column`; it was
  reported at `id.field`.
- `XGenerated` no longer writes `nullable = false` on the column of the identifier: a primary key
  is never null.
- The test of a repository that tib writes for a new entity no longer carries
  `@AutoConfigureTestDatabase(replace = Replace.NONE)`: Spring Boot 4 keeps the PostgreSQL of
  Testcontainers without it.
- The help of `--ddl` says what the option reads: the `CREATE TABLE` and `ALTER TABLE` statements
  of the DDL, as in the schema that `pg_dump` writes, and that tib skips the others.

### Fixed

- The tests of an entity with a field named as the entity, such as a table `city` with a column
  `city`, compile: `XSamples` built the entity with itself as the value of that field.
- A table or a column of DDL that is named with underscores alone, such as `_`, is a problem of
  the DDL; tib failed on it.
- A column of DDL whose check or generating expression has `NOT NULL` in it, as in
  `CHECK (n IS NOT NULL OR ...)`, is no longer read as a column that is `NOT NULL`.
- A `CREATE TABLE` with a `LIKE` clause among its columns is refused as a table created from
  another one; it gave an entity with only the columns that the statement names.
- `SERIAL2`, `SERIAL4` and `SERIAL8` in DDL are the serial types that they name; they were
  problems.

## [0.3.0] - 2026-11-11

### Added

- The generated service answers a request that breaks a constraint of the database, such as a
  unique value that is taken or a row that others refer to, with `409 Conflict` as a problem
  detail, instead of `500`. `tib generate entity` writes `ConflictHandler` once into the package
  of the application, with the first entity that has an API. The API test of an entity with a
  unique value checks it.
- `tib init service` writes the Gradle wrapper into the new service, of the Gradle that tib is
  built with, so the service needs no Gradle of its own. Its files are the developer's, written
  once. A file that is not text, such as the jar of the wrapper, shows in the diff of `--dry-run`
  as a line that says it differs, as in git, and `gradlew` is written as a file to run.
- `tib generate entity` writes `XSamples` among the tests of an entity, on every run: builders of
  the entity and of its DTO with the first or the second sample values, which the generated tests
  share. A test changes one value through the builder, and the samples follow the spec when a
  field is added.
- A run whose spec changes the columns of a table that has its migration says so on the standard
  error, with the diff from the migration that tib wrote to the one it would write now, and names
  the migration of a table that left the spec. When the spec generates a migration that is there
  already or that tib wrote before, as for a table that comes back to the spec, the run names it,
  with the diff from the file as it is to the one the spec generates now. The migrations stay as
  they are; changing, dropping or creating the table again is a migration of yours.

### Changed

- Breaking: the default pack generates Spring Boot 4.1 services, instead of Spring Boot 3.5: the
  modular starters of Spring Boot 4, Flyway through its starter, Testcontainers 2 and Jackson 3 in
  the tests. The state file names the stack that the pack generates for, and `tib generate entity`
  refuses a service that was generated for another one, which every service of an earlier tib
  was, also in a dry run, and names the tib to go on with; `tib status` still reads it.
- Breaking: a run whose spec no longer generates a file, as when an entity leaves the spec or
  loses its DTO or its API, deletes it, instead of forgetting it and leaving it on disk: a file
  written on every run always, and a file written once when it is as tib wrote it. A file written
  once that you edited stays, and the run names it on the standard error and forgets it: it is
  yours alone from then on. A migration always stays. `--dry-run` shows the deletions in its diff,
  and the run names the files it deleted.
- Breaking: `tib status` reads the spec files and the DDL that the state file names again, and
  exits with 1 when one changed since tib read it, until tib runs with it again. It is listed
  under `Read from:` as `changed`; one that is no longer there is listed as `absent`, and is no
  problem.
- Breaking: the first line of `XGenerated` is `Written by tib on every run. Do not edit: your
  changes would be lost.`, as in every other file written on every run, without `Your code goes in
  X.` after it.
- The tests of a generated service run against PostgreSQL 18 instead of 17.
- The Java that the default pack generates is formatted with google-java-format, its imports
  sorted and the unused ones left out, so that the formatter leaves the files of tib's as tib
  wrote them; `AGENTS.md` says which version. The packages carry the compiler of the JDK that it
  needs, without the commands `javac` and `serialver`, and are about 13 MB larger; their start-up
  cache is trained on a dry run of `tib init service`.
- `tib status` lists the files of tib's that differ under `Files:`, and says in a line of its own
  when every file that the spec was read from is as tib read it. Its help is one paragraph, and
  names exit code 0 as `Nothing that tib needs differs.`
- The state file forgets the spec files and the DDL that the spec no longer comes from, and the run
  names them on the standard error. A run with a spec file keeps only that file, as the spec comes
  from it alone; any other run forgets those that are no longer there, such as DDL that was moved.
  The spec keeps what was read from them.
- The `AGENTS.md` of a new service says that `tib status` also checks the spec files and the DDL,
  how an entity is removed, that the Gradle wrapper builds the service and that its files, which
  have no first line of tib's, are yours, and names the three first lines of a file of yours and
  `ConflictHandler`.
- The help of `--dry-run` reads `Prints what the run would change, as a diff, and changes
  nothing.`, as a dry run shows deletions too, and that of `--allow-dirty` starts with `Writes`.
- The state file records a file that was there before tib wrote it when it is as tib generates
  it, as it records the files that tib wrote; `tib generate entity` deletes it with its entity,
  and `tib status` lists it when it is removed.

### Fixed

- A file that is not UTF-8 text is reported as `cannot read spec.yaml: not UTF-8 text`, instead
  of `cannot read spec.yaml: Input length = 1`.
- A run that fails while it writes or deletes leaves the state file as it was, so that the next
  run, with `--allow-dirty` while what it wrote is not committed, finishes what it left: the state
  file is written last, and records every file that is as tib generates it, written by that run or
  there already.
- A file that the developer owns and that cannot be read ends the run with `cannot read` and its
  path, also a dry run, instead of being taken for a file that is there.
- The lines of one message end with a line feed on every platform; on Windows, a refusal with
  its advice mixed two line ends.
- DDL that creates a table from another one, as `CREATE TABLE … AS SELECT` or `LIKE` does, is
  reported as a problem of the DDL, instead of ending tib with an exception.
- The tests and the comments that tib generates say `an` before an entity whose name starts with
  a vowel, as in `saveWhatAnOrderNeeds`.

## [0.2.0] - 2026-11-05

### Added

- The state file records the spec files and the DDL that the spec of the service was read from,
  under `inputs`, each with its path relative to the service and the SHA-256 of its content when
  tib read it. A file read again takes the place of its earlier record. A state file written by
  this version cannot be read by tib 0.1.0.
- `tib generate entity` writes the tests of every entity, once, next to it under `src/test/java`:
  a test of its repository and one of its API over HTTP, both against PostgreSQL in a container,
  and a test of its service with the repository and the mapper mocked. An entity without a service
  gets the test of its repository only.
- `tib status` says which of the files that tib wrote into a service are no longer as it wrote
  them: a file of tib's that was edited or is missing, which `git checkout` restores, or the next
  run of tib writes again, with `--allow-dirty` while the change is not committed; and a file of
  yours that was removed. It exits with 1 when a file of tib's differs, so that a build can check
  it.
- A state file that a newer tib wrote is refused, with `.tib/state.yaml was written by tib 0.3.0,
  which is newer than this tib 0.2.0; upgrade tib`, instead of being read by an older tib and its
  older pack.
- `tib init service` writes a `.gitattributes` that keeps the line feeds of text files on every
  platform, so that a clone on Windows with `core.autocrlf` does not make every file of tib's
  differ from what tib wrote, and keeps batch files such as `gradlew.bat` in CRLF, which cmd.exe
  expects. In a service scaffolded by 0.1.0, add `* text=auto eol=lf`, `*.bat text eol=crlf` and
  `*.cmd text eol=crlf`, in this order, to your `.gitattributes`.
- A page of the documentation on the state file: its keys, the owners of a file, and which
  versions of tib read it.

### Changed

- Breaking: `tib init service` and `tib generate entity` refuse to write into a service whose
  files that git tracks have uncommitted changes, so that the changes of tib can be told apart, and
  exit with 1 where 0.1.0 wrote. Commit or stash your changes first, or pass `--allow-dirty`, which
  writes as before, with a warning. Untracked files, and a dry run, are not refused.
- Breaking: a bug in tib exits with 3, with its stack trace on the standard error; it exited with
  1, the code of a problem of the spec, the DDL, the pack or the project, which `tib status` also
  gives when a file of tib's differs. A script that treats every code other than 0 as a failure
  needs no change.
- Breaking: the default pack has the version of the tib it ships with, so that its version changes
  when what it generates can change. A spec that pins `pack: default@0.1.0` is refused by this
  version with `there is no pack 'default@0.1.0'`; pin the version of the tib you run, or write
  `pack: default`. The pin is in `pack:` of the spec file. A service that
  `tib generate entity --ddl` added entities to also has it in the spec inside `.tib/state.yaml`:
  change the `pack:` line under `spec:` there once, by hand; a run with `--spec` would forget the
  files of the entities that the spec file lacks.
- `TestcontainersConfiguration` of a new service is public, so that the tests of the entities in
  their own packages can import it. In a service scaffolded by 0.1.0, make it public before
  running `tib generate entity` again, or the new tests do not compile.
- `.tib/state.yaml` starts with a comment that says that tib writes it on every run and that it
  is not to be edited, as the other files of tib's say who owns them.
- The `AGENTS.md` of a new service says what `tib status` does, that tib writes only into a
  service whose tracked files are committed, and that `XGenerated`, `XDto` and `XMapper` stay out
  of a code formatter.
- The names that tib adds to a table whose name has no upper-case letter are in lower case too:
  the audit columns `created_at` and `updated_at`, the sequence `<table>_seq` and the index
  `<table>_<column>_idx`, as in a service read from DDL in lower case.
- `tib` starts faster and uses less memory: `tib --version` takes about a third of the CPU time
  of 0.1.0 and 60% of its memory, and the packages are about 9 MB smaller.
- The documentation site opens on the latest release, under `tib/latest/`, and the version of
  `main`, which documents what is not released yet, is marked as a prerelease.
- The messages on the standard error have one shape: the problem, then what to do about it on a
  line of its own. A problem of a state file starts with the directory of the service, and a
  directory that is not a tib project is told to run `tib init service` first.

### Fixed

- A state file that lacks a value tib needs, such as the path or the owner of a file, is refused
  with every problem it has instead of failing with a stack trace. Each problem names its value by
  the path from the root of the file, as in `files[2].owner` or `spec.entities[0].name`, the way a
  problem of a spec file does.
- A run whose spec no longer has an entity forgets the files of tib's that it generated for that
  entity, instead of keeping them in the state file for good, and names them on the standard
  error. They stay on disk as they are, yours to delete, and `tib status` no longer reports them.
- `tib init service --spec` in a directory that is a tib project already says so, instead of
  reporting a problem of the spec first.

## [0.1.0] - 2026-11-02

### Added

- `tib --version` prints the version, also after a subcommand, as in `tib init --version`.
- The help of every command and the command reference list the exit codes: 0 done or nothing to
  change, 1 a problem of the spec, the DDL, the pack or the project, 2 a wrong command line.
- Packages with their own Java runtime: a `.deb` without dependencies on X11, ALSA or FreeType and
  an `.msi`, that put `tib` on the PATH, and a portable zip for Linux and Windows.
- A start-up cache that the installers build where tib is installed, so that `tib` starts in less
  than half the time; the portable zip carries the script that builds it.
- An APT repository on GitHub Pages for Debian and Ubuntu.
- OpenPGP signatures for the files of a release and for the APT repository; the install page shows
  the key and how to verify a download.
- The documentation site, with a version for `main` and one for each release, the command
  reference generated from the commands and the properties of the spec from its schema.
- The spec file: YAML that describes a service and its entities, and `tib schema`, which prints
  its JSON Schema for completion and validation in an editor.
- `tib init service` scaffolds a Spring Boot 3 service with JPA, PostgreSQL, Flyway and
  Testcontainers from its name or its spec, with `--dry-run` to see the files as a diff before
  anything is written, and `.tib/state.yaml`, the record of what tib wrote.
- `AGENTS.md` in the scaffolded service: how to build and run it, and which files tib owns.
- `Test<Application>` in the scaffolded service, so that `./gradlew bootTestRun` runs the service
  against PostgreSQL in a container.
- The scaffolded service answers errors as problem details and serializes pages through DTOs.
- `tib generate entity` adds the entity layer of the spec's entities to the service: the mapped
  superclass with the columns and the entity, the repository, the DTO and the mapper, the service
  and the controller, and the Flyway migration that creates the table.
- The migration of an entity indexes the column of each many-to-one relation.
- `tib generate entity --ddl` reads the entities from the tables of SQL DDL: the `CREATE TABLE`
  statements and the keys that `ALTER TABLE` adds.
- Running `tib generate entity` again changes nothing and prints `Nothing to change.`; no file is
  ever left half written.

### Changed

- The spec refuses two members of an entity on one column and two entities on one table, in any
  case.
- The spec refuses Java keywords as names, also where a name becomes a package, and words that
  PostgreSQL reserves as tables and columns.
- The spec refuses a cycle of relations between entities; a relation of an entity to itself is
  allowed.
- The spec refuses a member that the generated code has already: `id` when the identifier has
  another field, `createdAt` and `updatedAt` in an audited entity, and the DTO's member for the
  target of a relation, as in `depotId`.
- The JSON Schema gives `tib` a maximum of 1, and a later version is refused as `tib: 2 is above 1`.
- The `events` option of an entity is gone, and a spec that sets it is refused.
- The `tib` key of the state file is the version of tib that wrote it, as in `tib: 0.1.0`.
- The repository of an entity is written once, when the entity is created, and is yours from then
  on.
- `AGENTS.md` is written once, when the service is created, and is yours from then on.
- The scaffolded service names its database and its user with underscores, as in
  `vehicle_service`.
- `tib`, `tib init` and `tib generate` without a subcommand print `Missing required subcommand` and
  their usage to the standard error, and exit with 2.
- `tib init service` takes either a name or `--spec`; both, or neither, is a usage error with exit
  code 2.
- A dry run that changes nothing prints `Nothing to change.` to the standard error.
- A run that writes one file says `Wrote 1 file`.
- A file that cannot be read or written, an empty spec and a broken state file are reported in
  one line, as in `cannot read spec.yaml: no such file`, with exit code 1.
- `tib generate entity --spec` refuses the spec of another service or group than the project's,
  as in `spec.yaml is the spec of fleet-service; this project is vehicle-service`, with exit code
  1.
- A problem of the spec that the state file records names the state file, as in
  `.tib/state.yaml: the spec has a problem: ...`.

### Fixed

- The singular of a table such as `STATUSES` is `Status`, and a name of one letter has a plural.
- A string literal in DDL, such as a comment that says `not null`, no longer changes its column.
- A file written once that you removed stays removed on the next run.
- A new migration is numbered after timestamp, dotted and nested versions too.
- The answer to a create or an update carries the times that the database has after it.
- The generated mapper sets the relations, so a relation added to the spec later is set and a
  removed one leaves no code that does not compile.
- A `String` without a length gets 255 in its column annotation and its validation too, not only
  in its migration.
- A failed run leaves no staging files behind.
- A file that tib writes gets the permissions of any new file, not `rw-------`, and a file it
  replaces keeps its own.
- An argument of tib no longer reaches the configuration of Spring, so it cannot switch on the
  banner or the logging.
- A process started from the portable zip on Linux no longer fails for want of `jspawnhelper`'s
  execute bit.

[Unreleased]: https://github.com/mryakar/tib-cli/compare/v0.4.0...HEAD
[0.4.0]: https://github.com/mryakar/tib-cli/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/mryakar/tib-cli/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/mryakar/tib-cli/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/mryakar/tib-cli/releases/tag/v0.1.0
