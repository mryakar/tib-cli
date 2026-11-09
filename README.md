# tib

The Idempotent Boilerplate: a scaffolding CLI for Spring Boot services.

## What it does

- `tib init service` scaffolds a Spring Boot 3 service with JPA, PostgreSQL, Flyway and
  Testcontainers, from its name or from a spec file.
- `tib generate entity` adds the entity layer of the spec's entities, or of the tables of SQL DDL:
  the entity, repository, DTO, mapper, service, controller and Flyway migration, with tests of
  the repository, the service and the API.
- In a git repository, tib writes only into a service whose tracked files are committed, unless
  `--allow-dirty` says so.
- `--dry-run` shows the diff and writes nothing; running `tib generate entity` again changes
  nothing and prints `Nothing to change.`. Every generated file says in its first line who owns
  it: the generator or you.
- `tib status` says which files of tib's were edited or are missing, and which spec files and DDL
  changed since tib read them, and exits with 1 if any did.

The [documentation](https://mryakar.github.io/tib-cli/) walks through a first service and lists
every command; the [roadmap](ROADMAP.md) says what is done and what comes next.

## Install

On Debian and Ubuntu from the APT repository, on Windows with the `.msi`, or from a portable zip,
each with its own Java runtime: the
[install page](https://mryakar.github.io/tib-cli/tib/latest/install.html) says how.

## Building

tib needs JDK 21. [CONTRIBUTING.md](CONTRIBUTING.md) says how to build and test it.

## License

[Apache License 2.0](LICENSE).
