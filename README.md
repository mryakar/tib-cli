# tib

> The Idempotent Boilerplate

A scaffolding CLI for Spring Boot services: it sets up a service, adds an entity layer to it from a
DDL or a spec, and runs again on the same project without breaking anything.

## Status

tib is at the start. What works today is the pipeline around it: packages for Linux and Windows
with their own Java runtime, a [documentation site](https://mryakar.github.io/tib-cli/), the spec
file with its JSON Schema, and two commands, `tib --version` and `tib schema`. The generators come
next; the [roadmap](ROADMAP.md) says in what order. Nothing is released yet.

## What it will do

- `tib init service` sets up a Spring Boot service from a template pack: the default pack is plain
  Spring Boot with JPA, PostgreSQL, Flyway and Testcontainers.
- `tib generate entity Vehicle --ddl vehicle.sql` adds the entity, repository, service, mapper,
  DTOs, controller, migration and tests for a table, from its DDL or from a YAML spec.
- `--dry-run` shows the diff and writes nothing. Running a command twice changes nothing the
  second time. Generated files say who owns them: the generator, you, or both through markers.
- Every command runs without questions when given a spec, and prints machine-readable output, so
  that scripts and agents can drive it.

## Install

Every package embeds its own Java runtime. On Debian and Ubuntu, from the APT repository:

```bash
curl -fsSL https://mryakar.github.io/tib-cli/apt/tib.gpg | sudo tee /usr/share/keyrings/tib.gpg >/dev/null
echo "deb [signed-by=/usr/share/keyrings/tib.gpg] https://mryakar.github.io/tib-cli/apt stable main" | sudo tee /etc/apt/sources.list.d/tib.list
sudo apt update && sudo apt install tib
```

On Windows, run the `.msi` of a [release](https://github.com/mryakar/tib-cli/releases). A
portable zip for either platform needs no installation. The
[install page](https://mryakar.github.io/tib-cli/tib/main/install.html) has the details.

## Building

tib needs JDK 21. `./gradlew build` compiles, formats, tests and checks the coverage;
`./gradlew packages` builds the packages of your platform. [CONTRIBUTING.md](CONTRIBUTING.md)
has the rest.

## License

[Apache License 2.0](LICENSE).
