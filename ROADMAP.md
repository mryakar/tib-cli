# Roadmap

tib grows in phases. Each phase has a condition for being done; the version numbers are not
promised in advance.

| Phase | Scope | Done when |
|---|---|---|
| 0. Measure | How long the entity layer of a service takes by hand, on a reference service | The number is written down |
| 1. MVP | `init service` with a template pack, for Spring Boot or for plain Java, in one of three architectures; `generate entity` from a DDL or a YAML spec, with tests and a migration; a real `--dry-run`; idempotent runs; a state file and file ownership; `tib status`; `AGENTS.md` for the generated service; packages for Linux, Windows and macOS; the documentation site | A reference service is generated and extended from a DDL, twice, with the second run changing nothing |
| 2. Expand | Several services in one project; Kafka and Feign; OpenAPI and a live database as inputs; an Angular frontend from the same model; Gradle and Maven plugins | Each input and each output has a reference service |
| 3. Update | `tib upgrade` to a newer pack with a three-way merge; recipes that move a service to a newer stack | A service generated with version N upgrades to N+1 with a clean merge |
| 4. Assist | An optional, on-premise assistant that drafts a spec from a description; it writes no files | Switched off, tib behaves exactly as before |
| 5. Ecosystem | Template packs from others; a registry; integrations | A pack written outside this repository works unchanged |

Out of scope for the MVP: several services at once, the frontend, migrations for a changed
table, upgrades, the assistant, native images and a DSL of its own.

## Done

- `tib init service`, from a name, a spec file or the answers to its questions, with the Gradle
  wrapper and `AGENTS.md`: a service on Spring Boot 4 or in plain Java, in the anemic, DDD or
  hexagonal architecture, on Java 21 or 25, with the kinds of tests it chooses and, if it wants
  one, a `Dockerfile`.
- `tib generate entity` from a spec or from DDL, as `pg_dump --schema-only` writes it: the entity
  layer, its migration, its tests and the samples they share.
- `--dry-run`, a second run that changes nothing, and writing only into a service whose changes
  are committed.
- The state file, with the owner of every file and the spec files and DDL that the spec came
  from; the deletion of the files that the spec no longer generates, and a notice when the spec
  changes a table that has its migration; `tib status`.
- The packages for Linux, Windows and macOS, and the documentation site.

## Next

Phase 2.
