# Roadmap

tib grows in phases. Each phase has a condition for being done; the version numbers are not
promised in advance.

| Phase | Scope | Done when |
|---|---|---|
| 0. Measure | How long the entity layer of a service takes by hand, on a reference service | The number is written down |
| 1. MVP | `init service` with the default pack; `generate entity` from a DDL or a YAML spec, with tests and a migration; a real `--dry-run`; idempotent runs; a state file and file ownership; `tib status`; `AGENTS.md` for the generated service; packages for Linux and Windows; the documentation site | A reference service is generated and extended from a DDL, twice, with the second run changing nothing |
| 2. Expand | Several services in one project; Kafka and Feign; OpenAPI and a live database as inputs; an Angular frontend from the same model; Gradle and Maven plugins | Each input and each output has a reference service |
| 3. Update | `tib upgrade` to a newer pack with a three-way merge; recipes that move a service to a newer stack | A service generated with version N upgrades to N+1 with a clean merge |
| 4. Assist | An optional, on-premise assistant that drafts a spec from a description; it writes no files | Switched off, tib behaves exactly as before |
| 5. Ecosystem | Template packs from others; a registry; integrations | A pack written outside this repository works unchanged |

Out of scope for the MVP: several services at once, the frontend, migrations for a changed
table, upgrades, the assistant, native images, a DSL of its own, and more than one stack.

What is done today: `tib init service`, and `tib generate entity` from a spec or from DDL, with
`--dry-run`, the state file, which names the spec files and the DDL that the spec came from, the
owner of every file and `AGENTS.md`; the tests of every entity; `tib status`; writing only into a
service whose changes are committed; the packages for Linux and Windows; the documentation site;
the first release.

## Next

These come after the MVP, in no fixed order:

- A notice when the columns of a table changed after its migration was written.
- A default pack on Spring Boot 4.
- The Gradle wrapper in the generated service.
- The generated Java formatted with google-java-format by the default pack, so that a formatter
  run on the service leaves the files of tib's as tib wrote them.
- One builder of sample entities per entity, which the generated tests share.
- PostgreSQL 18 for the generated services.
