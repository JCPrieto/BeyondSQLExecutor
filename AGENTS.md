# Repository Guidelines

## Project Structure & Module Organization

- `src/main/java/`: application source (main class: `es.jklabs.BeyondSQLExecutor`, Swing UI under `es.jklabs.gui`).
- `src/main/resources/`: runtime assets
    - `i18n/*.properties`: UI strings and error messages
    - `img/icons/`: bundled icons
- `build/`: Gradle outputs (generated; don’t hand-edit).

## Build, Test, and Development Commands

Prereqs: Java 21 (see `README.md`). Prefer the bundled Gradle wrapper (`./gradlew` / `gradlew.bat`) for consistent
builds across environments; alternatively, you can run tasks from IntelliJ’s Gradle tool window.

```bash
./gradlew build        # compile + run tests
./gradlew test         # run JUnit 5 tests
./gradlew run          # launch the app (application plugin)
./gradlew installDist  # create runnable distribution under build/install/
./gradlew distZip      # zip distribution under build/distributions/
```

## Coding Style & Naming Conventions

- Java: 4-space indentation, braces on the same line, keep methods small and single-purpose.
- Naming: packages `lowercase`, classes `PascalCase`, methods/fields `camelCase`, constants `UPPER_SNAKE_CASE`.
- UI text: prefer `src/main/resources/i18n/*.properties` keys over hard-coded strings.
- Assets: keep icons under `src/main/resources/img/icons/` and reference them via the classpath.

## Testing Guidelines

- Framework: JUnit 5 (`test { useJUnitPlatform() }` in `build.gradle`).
- Location: add new tests under `src/test/java/` and name them `*Test` (e.g., `UtilidadesConfiguracionTest`).
- Focus: unit-test parsing/serialization and utility logic; UI changes should include a brief manual test note in the
  PR.

## Commit & Pull Request Guidelines

- Commit messages in history are short and usually Spanish (often “Corrección…”, “Readme”, or version/changelog notes).
  Keep subjects concise and action-oriented.
- PRs should include: what/why, how to verify, and screenshots/GIFs for UI changes; note the OS tested (Windows/Linux).
- Do not commit secrets or user configuration files; treat connection settings and credentials as sensitive.

## Security & Configuration Tips

- Local app config is stored in `~/.BeyondSQLExecutor/connections.json` (legacy migration may read old `config.json`
  once); avoid committing exported configs containing
  hosts/users.
- AWS IAM auth uses local profiles (typically `~/.aws/credentials`).
- Release automation for Linux APT relies on `APT_REPO_DISPATCH_TOKEN` (GitHub secret) and optional
  `APT_REPO_OWNER`/`APT_REPO_NAME` (repo variables); never hard-code or expose these values in code/logs.
- Release workflow security: keep explicit guards to ensure release jobs run only for successful `push` events to
  `main` from the same repository (never from forks), and prefer restrictive checkout options.
- Exported project ZIPs are portable and contain `connections.json` only (no `.secure/` vault files); treat exported
  files as sensitive.
- On Linux, OS secure storage depends on `secret-tool` (`libsecret-tools` package).
