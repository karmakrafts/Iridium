## [Unreleased]

## [1.13.2]

### Changed

- Updated to Gradle 9.6.1
- Updated to Karma Conventions 1.18.3
- Updated to OSHI 7.4.0
- Updated to NMCP 1.6.1

## [1.13.1]

### Changed

- Updated to Gradle 9.6.0
- Updated to Karma Conventions 1.18.1
- Updated to OSHI 7.3.2
- Migrated to NMCP based Maven Central publishing

## [1.13.0]

### Added

- `CompilerPipelineBuilder.extraLibrary` for specifying extra KLIBs for the created pipeline
- `CompilerPipelineBuilder.extraLibraries` for specifying extra KLIBs for the created pipeline

### Changed

- Fixed resolution of builtin platform libraries (`kotlin.js`, `kotlinx.cinterop` etc.) for Kotlin Native, JS and WASM

## [1.12.0]

### Added

- Added `CompilerTarget` enum to define the compilation target of a test pipeline
- Added support for Kotlin/Native compilation
- Added support for Kotlin/JS compilation
- Added support for Kotlin/WASM compilation

### Changed

- Updated to Kotlin 2.4.0
- Updated to Karma Conventions 1.17.0
- Downgraded to Gradle 9.4.1 because of IDEA compatibility regression

## [1.11.1]

### Changed

- Updated to Kotlin 2.3.21
- Updated to Karma Conventions 1.16.1
- Updated to Gradle 9.5.0
- Updated to JUnit 5.14.4

## [1.11.0]

### Changed

- Updated to Kotlin 2.3.20
- Updated to Gradle 9.4.1
- Updated to Karma Conventions 1.15.1
- Updated to JUnit 5.14.3
- Updated to Dokka 2.2.0
- Added automatic changelog