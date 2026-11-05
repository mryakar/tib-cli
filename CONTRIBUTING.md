# Contributing to tib

Thank you for your interest in tib. The project is at its start, so please open an issue to discuss
a change before you start working on it.

## Building

tib needs JDK 21; the packages embed JDK 25, which Gradle downloads when it needs it.

| Command | What it does |
|---|---|
| `./gradlew build` | Formatting check, tests, the acceptance test (see Tests), coverage verification, the generated documentation |
| `./gradlew spotlessApply` | Formats the code and adds license headers |
| `./gradlew build -PtestJavaVersion=25` | Runs the tests on another JDK (the code is still compiled for 21) |
| `./gradlew test updateSnapshots --continue` | Replaces the snapshots under `src/test/snapshots` with what the unit tests wrote; review the change with `git diff` |
| `./gradlew packages` | Builds the installer of your platform (`.deb` or `.msi`) and the portable zip, in `tib-cli/build/packages` |
| `./gradlew :tib-cli:antoraContent` | Generates the command reference, the tables of the spec and its schema for the documentation |

The documentation site needs Node: in `docs/`, run `npm ci`, then `npx antora antora-playbook.yml`.
Antora runs the documentation tasks of the build itself and writes the site to `docs/build/site`.

## Code

- Format with `./gradlew spotlessApply` before committing. Java follows google-java-format; Groovy
  tests use the same indentation; Gradle scripts follow ktfmt. Spotless and google-java-format are
  updated together, and the code that a new version formats differently changes in a commit of its
  own.
- Every source file starts with the Apache-2.0 license header. Spotless adds it.
- Classes go in the package of their concept, not in packages named after a layer (`model`,
  `util`).
- tib does not depend on Spring; look for a solution in the language and the JDK before reaching
  for a framework.

## Tests

- Tests are written with [Spock](https://spockframework.org/) in `src/test/groovy`, and the
  acceptance test in `src/acceptanceTest/groovy`.
- Every change comes with tests. The build requires 100% instruction, branch and line coverage.
- Generators are tested three ways: a snapshot of the files they write, a second run that changes
  nothing, and the acceptance test, which generates three services, from the example spec, from
  its DDL and from the corner spec of the snapshots, and builds each with Gradle; where Docker runs
  Linux containers, the build runs the tests of the service against PostgreSQL in a container,
  elsewhere it compiles the service and its tests.
- A change of a template, of the state file or of the schema of the spec changes the snapshots
  under `src/test/snapshots` of its module: `./gradlew test updateSnapshots --continue` replaces
  them with what the tests wrote. Review the change with `git diff`, and commit the snapshots with
  the change that made them, in a commit of its own. An update of the Spring Boot that the default
  pack generates comes in a pull request of its own, with the acceptance test green.

## Design rules

- `--dry-run` writes nothing. A change that breaks this is not merged.
- The same input, pack and version give the same output. Nothing random, no timestamps, unless a
  command asks for them.
- Every generated file says in its header who owns it: the generator or the developer.

## Documentation

The site under `docs/` is built with Antora from AsciiDoc. The command reference is generated from
the commands by the build, and the tables of the spec's properties from its schema; do not edit
them by hand. A change that adds or changes a command brings its documentation with it; a new
command needs its page under `reference/` and a line in `nav.adoc`.

## Commits

Commits follow [Conventional Commits](https://www.conventionalcommits.org/):

```text
<type>(<optional-scope>)!: <description>
```

Allowed types: `feat`, `fix`, `docs`, `refactor`, `chore`, `test`, `build`, `ci`, `perf`. Mark
breaking changes with `!` or a `BREAKING CHANGE:` footer. Keep commits small; each commit should
contain only related changes. CI checks every commit message in a pull request.

## Pull requests

- `main` is protected; all changes go through pull requests, one at a time.
- Pull requests are merged with **rebase and merge** only, so every commit lands on `main` as-is.
  Rebase your branch onto `main` instead of merging `main` into it.
- CI builds and tests on Linux and Windows, builds the packages and installs each of them.

## Releases

Between two releases, `gradle.properties` has the version of the next release with `-SNAPSHOT`,
as in `version=0.3.0-SNAPSHOT`, and the site shows the documentation of `main` as a prerelease.

A release starts with a pull request of its own that sets `version=X.Y.Z` in `gradle.properties`,
gives the `[Unreleased]` section of [CHANGELOG.md](CHANGELOG.md) the version and the date, and
sets the versions that the getting-started page prints. The maintainer then releases the version
from a `vX.Y.Z` tag that matches `gradle.properties` and has a section in the changelog. The
release workflow builds the packages, drafts the GitHub Release with them, their checksums and
signatures, and the maintainer publishes it. The APT repository on GitHub Pages picks up the
`.deb` of every published release. After the release, a commit sets `version=X.Y+1.0-SNAPSHOT`.

## License

By contributing, you agree that your contributions are licensed under the
[Apache License 2.0](LICENSE).
