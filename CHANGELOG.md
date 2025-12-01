# Changelog

All notable changes to JLessC will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- GitHub Actions CI workflow with support for multiple JDK versions (8, 11, 17, 21, 25)
- GitHub Actions release workflow for automated releases
- Support for snapshot tags (e.g., `v1.16.0-SNAPSHOT`) in release workflow
- Gradle 9 compatibility
- `gradle.properties` for version management
- Modern Maven publishing using `maven-publish` plugin
- Nexus publish plugin for Sonatype/Maven Central publishing
- Support for both `publishToMavenLocal` and `publishToSonatype`
- Automatic signing for Maven Central releases
- Compatibility validation step in CI to ensure Gradle-Java version matching

### Changed
- Modernized build system from old `maven` plugin to `maven-publish`
- Updated Node.js plugin from `com.moowork.node` to `com.github.node-gradle.node`
- CI matrix simplified to use latest Gradle version per Java version (reduced from 13 to 5 combinations)
- Updated sourceCompatibility from Java 7 to Java 8
- Replaced deprecated `archivesBaseName` with `base.archivesName` for Gradle 9 compatibility
- Moved Java configuration (sourceCompatibility, encoding) into `java {}` block for Gradle 9
- Replaced `org.gradle.util.VersionNumber` with `JavaVersion.current()` for Gradle 9 compatibility

### Fixed
- Node.js setup in CI workflow (removed npm caching for non-existent package-lock.json)
- Gradle 9 compatibility issues with Java version detection
- Gradle 9 compatibility issues with sourceCompatibility property
- Gradle 9 compatibility issues with archivesBaseName property

### Infrastructure
- Added `.gitignore` entries for IDE files (.classpath, .project, .settings/)
- Updated LICENSE file name
- Removed Travis CI dependency (migrated to GitHub Actions)

## [1.15] - 2024

### Fixed
- Close resource files on errors to prevent resource leaks

## [1.14] - 2024

### Fixed
- Disable the removing of units for zero values (fraction unit "fr" is required also for 0 values)

### Changed
- Disabled coverage plugin temporarily due to incompatibility with current Gradle version
- Set explicit artifactId on publishing for Sonatype (lowercase requirement)

## [1.13] - 2024

### Changed
- Migrated to `ossrh-staging-api.central.sonatype.com` for Maven Central publishing
- Updated to work with Gradle 8
- Updated Gradle node plugin

### Added
- Implemented `isDefined` function (see #84)

## [1.12] - 2024

### Changed
- Made parameter 'contrast_color' optional for the 'colorize-image' function

## [1.11] - 2024

### Changed
- Removed reference to org.springsource.ide.eclipse.gradle.core.nature
- Added bubbling for @container like @media

## [1.10] - 2024

### Fixed
- Fixed base64 problems

## [1.9] - 2024

### Changed
- Increment version (see #62)

## [1.8] - 2024

### Changed
- Updated build configuration

## [1.7] - 2024

### Changed
- Corrected required Java version documentation
- Updated reference data for Java 9 for all Java versions
- Added special reference for Java 8

## [1.6] - 2024

### Changed
- Updated build system to support more JDK versions

## [1.5] - 2024

### Changed
- Last version to support Java 7

---

[Unreleased]: https://github.com/i-net-software/jlessc/compare/v1.15...HEAD
[1.15]: https://github.com/i-net-software/jlessc/compare/v1.14...v1.15
[1.14]: https://github.com/i-net-software/jlessc/compare/v1.13...v1.14
[1.13]: https://github.com/i-net-software/jlessc/compare/v1.12...v1.13
[1.12]: https://github.com/i-net-software/jlessc/compare/v1.11...v1.12
[1.11]: https://github.com/i-net-software/jlessc/compare/v1.10...v1.11
[1.10]: https://github.com/i-net-software/jlessc/compare/v1.9...v1.10
[1.9]: https://github.com/i-net-software/jlessc/compare/v1.8...v1.9
[1.8]: https://github.com/i-net-software/jlessc/compare/v1.7...v1.8
[1.7]: https://github.com/i-net-software/jlessc/compare/v1.6...v1.7
[1.6]: https://github.com/i-net-software/jlessc/compare/v1.5...v1.6

