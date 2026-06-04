# Changelog

All notable changes to this project will be documented in this file.

## [2.0.1] - 2026-06-04

### Fixed
- Hardened the WebView bridge and remote injection flow with origin checks and SHA-256 pinning.
- Disabled release debuggability and removed overbroad backup, permission, and provider exposure.
- Fixed settings button accessibility by implementing thread-safe URL caching on the UI thread to resolve background thread crashes.
- Made settings selector in Injector.js more robust to adapt to dynamic Instagram web layouts.

## [2.0.0] - 2026-03-06

### Highlights
- Major update with new features and improvements.
- Integrated GitHub Actions for automated building, signing, and releasing of APKs.

### Added
- **UI Improvements**: Refined various screens for a better user experience.
- **CI/CD Pipeline**: Added automated release workflow.
