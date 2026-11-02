# tib

The Idempotent Boilerplate: a scaffolding CLI for Spring Boot services.

## What it does

- `tib init service` scaffolds a Spring Boot 3 service with JPA, PostgreSQL, Flyway and
  Testcontainers, from its name or from a spec file.
- `tib generate entity` adds the entity layer of the spec's entities, or of the tables of SQL DDL:
  the entity, repository, DTO, mapper, service, controller and Flyway migration.
- `--dry-run` shows the diff and writes nothing; running `tib generate entity` again changes
  nothing and prints `Nothing to change.`. Every generated file says in its first line who owns
  it: the generator or you.

The [documentation](https://mryakar.github.io/tib-cli/) walks through a first service and lists
every command; the [roadmap](ROADMAP.md) says what is done and what comes next.

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

tib needs JDK 21. [CONTRIBUTING.md](CONTRIBUTING.md) says how to build and test it.

## License

[Apache License 2.0](LICENSE).
