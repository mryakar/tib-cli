# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- The state file records the spec files and the DDL that the spec of the service was read from,
  under `inputs`, each with its path relative to the service and the SHA-256 of its content when
  tib read it. A file read again takes the place of its earlier record. tib 0.1.0 does not read a
  state file with `inputs`.
- `tib init service` and `tib generate entity` refuse to write into a service whose files that git
  tracks have uncommitted changes, so that the changes of tib can be told apart; `--allow-dirty`
  writes anyway, with a warning. Untracked files, and a dry run, are not refused.
- `tib generate entity` writes the tests of every entity, once, next to it under `src/test/java`:
  a test of its repository and one of its API over HTTP, both against PostgreSQL in a container,
  and a test of its service with the repository and the mapper mocked. An entity without a service
  gets the test of its repository only.
- `tib status` says which of the files that tib wrote into a service are no longer as it wrote
  them: a file of tib's that was edited or is missing, which tib replaces on its next run, and a
  file of yours that was removed. It exits with 1 when a file of tib's differs, so that a build
  can check it.
- A state file that a newer tib wrote is refused, with `.tib/state.yaml was written by tib 0.3.0,
  which is newer than this tib 0.2.0; upgrade tib`, instead of being read by an older tib and its
  older pack.
- `tib init service` writes a `.gitattributes` that keeps the line feeds of text files on every
  platform, so that a clone on Windows with `core.autocrlf` does not make every file of tib's
  differ from what tib wrote. In a service scaffolded by 0.1.0, add `* text=auto eol=lf` to your
  `.gitattributes`.

### Changed

- `TestcontainersConfiguration` of a new service is public, so that the tests of the entities in
  their own packages can import it. In a service scaffolded by 0.1.0, make it public before
  running `tib generate entity` again, or the new tests do not compile.
- `.tib/state.yaml` starts with a comment that says that tib writes it on every run and that it
  is not to be edited, as the other files of tib's say who owns them.
- The default pack has the version of the tib it ships with, so that its version changes when what
  it generates can change. A spec that pins `pack: default@0.1.0` is refused by this version with
  `there is no pack 'default@0.1.0'`; pin the version of the tib you run, or write `pack: default`.
- The names that tib adds to a table whose name has no upper-case letter are in lower case too:
  the audit columns `created_at` and `updated_at`, the sequence `<table>_seq` and the index
  `<table>_<column>_idx`, as in a service read from DDL in lower case.

### Fixed

- A state file that lacks a value tib needs, such as the path or the owner of a file, is refused
  with every problem it has instead of failing with a stack trace. Each problem names its value by
  the path from the root of the file, as in `files[2].owner` or `spec.entities[0].name`, the way a
  problem of a spec file does.
- A run whose spec no longer has an entity forgets the files of tib's that it generated for that
  entity, instead of keeping them in the state file for good. They stay on disk as they are, yours
  to delete, and `tib status` no longer reports them.

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

[Unreleased]: https://github.com/mryakar/tib-cli/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/mryakar/tib-cli/releases/tag/v0.1.0
