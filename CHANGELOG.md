# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased / Snapshot]

### Changed
- Provide error message when fail calculating the correction [#24](https://github.com/ie3-institute/powerflow/issues/24)

## [0.4]

### Added
- Added dependabot workflow and `CODEOWNERS` [#229](https://github.com/ie3-institute/powerflow/issues/229)
- spotless scala3 formatting [#246](https://github.com/ie3-institute/powerflow/issues/246)

### Changed
- Removed Jenkins due to redundancy with GHA [#237](https://github.com/ie3-institute/powerflow/issues/237)
- Got rid of java implementation [#242](https://github.com/ie3-institute/powerflow/issues/242)
- Introduce new deployment scripts [#250](https://github.com/ie3-institute/powerflow/issues/250)

### Update
- Updating gradle to 9.1.0 [#248](https://github.com/ie3-institute/powerflow/issues/248)
- Updating Java to 21 [#251](https://github.com/ie3-institute/powerflow/issues/251)

## [0.3]

### Added
- Added Bao and Staudt to list of reviewers [#188](https://github.com/ie3-institute/powerflow/issues/188)
- Added semantic Versioning plugin to gradle [#218](https://github.com/ie3-institute/powerflow/issues/218)
- Implemented GitHub Actions Pipeline [#215](https://github.com/ie3-institute/powerflow/issues/215)

## Removed
- Removed dependency constraint [#221](https://github.com/ie3-institute/powerflow/issues/221)

### Updates
- Upgraded to `scala3` [#204](https://github.com/ie3-institute/powerflow/issues/204)
- Bumping gradle to 8.14

## [0.2]

### Changed
- Various updates to CI
- Updated dependencies
- Updating to gradle 8.9 [#136](https://github.com/ie3-institute/powerflow/issues/136)
- Fix spotless deprecations [#197](https://github.com/ie3-institute/powerflow/issues/197)

## [0.1]
### Added
- Initial project structure and code

### Changed
- Reduce log pollution for not provided node order
- Change logging framework to logback
- Add deployment capabilities

### Removed
- Spock testing framework

[Unreleased / Snapshot]: https://github.com/ie3-institute/powerflow/compare/0.4...HEAD
[0.4]: https://github.com/ie3-institute/powerflow/compare/0.3...0.4
[0.3]: https://github.com/ie3-institute/powerflow/compare/0.2...0.3
[0.2]: https://github.com/ie3-institute/powerflow/compare/0.1...0.2
[0.1]: https://github.com/ie3-institute/powerflow/releases/tag/0.1
