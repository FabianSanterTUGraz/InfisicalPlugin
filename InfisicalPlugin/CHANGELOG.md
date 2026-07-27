<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# InfisicalPlugin Changelog

## [Unreleased]

### Added

- Browser-based login flow with secure token storage via IntelliJ PasswordSafe
- HTTP client for the Infisical REST API to fetch secrets by project/environment/path
- Secret injection into Gradle, Spring Boot, and npm run configurations
- Settings UI with environment picker and login/logout action
- Automatic secret refresh on environment switch
- GitLab CI pipeline (build, test, plugin verifier)
- Setup documentation in README (CLI installation, abuscom instance connection, `.infisical.json`)

### Fixed

- Invalid/expired token handling now shows notifications instead of crashing
- Environment dropdown not refreshing after login or with an expired cached token
- HTTP callback server port leak during login
- Plugin verifier compatibility issues in `plugin.xml`

### Changed

- Renamed package/module to the real namespace (`com.abuscom.infisicalplugin`)
- Removed built plugin artifacts from version control
