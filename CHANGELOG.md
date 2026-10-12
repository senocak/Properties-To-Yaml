<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Properties-To-Yaml Changelog

## [Unreleased]
## [0.2.0] - 2026-10-11
### Added
- Preserve comments in both conversion directions: full-line and inline `# ...` comments in `.properties` files are carried into the generated YAML, and comments in `.yml` files are carried into the generated properties output
- Support multi-document YAML files (documents separated by `---`) when converting to `.properties`; documents after the first are separated by a `#---` comment line

### Changed
- Target IntelliJ Platform 2025.3.6.1 (`2025.3+` compatible, no until-build restriction)
- Upgrade Gradle Wrapper to `9.8.0`
- Dependencies - upgrade `org.jetbrains.kotlin.jvm` to `2.4.20`
- Dependencies - upgrade `org.jetbrains.intellij.platform` to `2.19.0`
- Dependencies - upgrade `org.yaml:snakeyaml` to `2.5`
- Actions now declare `ActionUpdateThread.BGT`

### Fixed
- Fix scalar values conflicting with nested keys of the same name (nested structure wins)
- Fix nested section headers rendering with flow-style braces (`key: {}`)
- Fix unit tests referencing a non-existent service method

## [0.0.1] - 2025-08-20

### Added

- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)

[Unreleased]: https://github.com/senocak/Properties-To-Yaml/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/senocak/Properties-To-Yaml/compare/v0.0.1...v0.2.0
[0.0.1]: https://github.com/senocak/Properties-To-Yaml/commits/v0.0.1
