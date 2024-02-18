# Changelog

All notable changes to this project will be documented in this file.

This project is forked from
[Mozilla's rust-android-gradle plugin](https://github.com/mozilla/rust-android-gradle).
The last version of the original project is
[0.9.3](https://github.com/mozilla/rust-android-gradle/commit/4fba4b9db16d56ba4e4f9aef2c028a4c2d6a9126).
The initial release of this project has started from 0.1.0, ignoring the original project's released versions.
For the changes from the original project,
see [CHANGELOG.md of the original project](https://github.com/mozilla/rust-android-gradle/blob/4fba4b9db16d56ba4e4f9aef2c028a4c2d6a9126/CHANGELOG.md).

## [Unreleased]

## [0.2.0] - 2024-02-18

### Added

- Add configuration block for product flavors.

### Changed

- Change the precedence order of cargo target directory to use (highest priority first):
  1. `targetDirectory` in `cargo` block in `build.gradle`.
  2. `CARGO_TARGET_DIR` environment variable.
  3. Default `${module}/target` directory.

### Fixed

- Fix local unittest for Android Gradle Plugin 8.2.

### Internal

- Use plugin DSL instead of legacy plugin application.
- Support incremental build of cargoBuildTask.
- Use lazy task registration API.
- Accelerate CI by caching gradle and cargo directories.
- Add instrumentation tests to the sample app.

## [0.1.0] - 2024-01-29

### Added

- Add configuration options and build for different profiles depending on android plugin's build type.
- Support Android Gradle Plugin 8.2/8.1/8.0 and Gradle 8.2/8.0.

### Changed

- Use GitHub package registry for publishing.

### Fixed

- Fix CI to support latest GitHub Actions environment.
