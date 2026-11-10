# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
  needs, without the commands `javac` and `serialver`, and are about 20 MB larger; their start-up
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

[Unreleased]: https://github.com/mryakar/tib-cli/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/mryakar/tib-cli/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/mryakar/tib-cli/releases/tag/v0.1.0
