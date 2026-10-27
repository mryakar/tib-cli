# Contributing to tib

Thank you for your interest in tib. The project is at its start, so please open an issue to discuss
a change before you start working on it.

## Building

tib needs JDK 21; the packages embed JDK 25, which Gradle downloads when it needs it.

| Command | What it does |
|---|---|
| `./gradlew build` | Formatting check, tests, coverage verification |
| `./gradlew spotlessApply` | Formats the code and adds license headers |
| `./gradlew build -PtestJavaVersion=25` | Runs the tests on another JDK (the code is still compiled for 21) |
| `./gradlew packages` | Builds the installer of your platform (`.deb` or `.msi`) and the portable zip, in `tib-cli/build/packages` |
| `./gradlew :tib-cli:antoraContent` | Generates the command reference of the documentation |

The documentation site needs Node: `npm ci` in `docs/`, then `npx antora antora-playbook.yml` there
writes the site to `docs/build/site`.

## Code

- Format with `./gradlew spotlessApply` before committing. Java follows google-java-format; Groovy
  tests use the same indentation; Gradle scripts follow ktfmt.
- Every source file starts with the Apache-2.0 license header. Spotless adds it.
- Classes go in the package of their concept, not in packages named after a layer (`model`,
  `util`).
- `tib-core` does not depend on Spring.
- Look for a solution in the language and the JDK before reaching for a framework.

## Tests

- Tests are written with [Spock](https://spockframework.org/) in `src/test/groovy`.
- Every change comes with tests. The build requires 100% instruction, branch and line coverage.
- Generators are tested three ways: a snapshot of the files they write, the generated project
  built for real, and a second run that changes nothing.

## Design rules

- `--dry-run` writes nothing. A change that breaks this is not merged.
- The same input, pack and version give the same output. Nothing random, no timestamps, unless a
  command asks for them.
- Every generated file says in its header who owns it: the generator, the developer, or both.

## Documentation

The site under `docs/` is built with Antora from AsciiDoc. The command reference is generated from
the commands by the build; do not edit it by hand. A change that adds or changes a command brings
its documentation with it.

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
- `./gradlew check` also generates the example service and builds it with Gradle, as a user would;
  where Docker runs, that includes the service's test against PostgreSQL in a container, elsewhere
  the build compiles the service and its test.

## Releases

The maintainer releases a version from a `vX.Y.Z` tag that matches `gradle.properties` and has a
section in [CHANGELOG.md](CHANGELOG.md). The release workflow builds the packages, drafts the
GitHub Release with them, their checksums and signatures, and the maintainer publishes it. The
APT repository on GitHub Pages picks up the `.deb` of every published release.

## License

By contributing, you agree that your contributions are licensed under the
[Apache License 2.0](LICENSE).
