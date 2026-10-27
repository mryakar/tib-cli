# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `tib --version` prints the version; `tib` alone prints its usage.
- Packages with their own Java runtime: a `.deb` and an `.msi` that put `tib` on the PATH, and a
  portable zip for Linux and Windows.
- An APT repository on GitHub Pages for Debian and Ubuntu.
- The documentation site, with the command reference generated from the commands.
- The spec file: YAML that describes a service and its entities, and `tib schema`, which prints
  its JSON Schema for completion and validation in an editor.
