# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

@AGENTS.md

## This fork

This repository is a personal fork of Voice (`origin` is `NuclleaR/Shelf-Audio`), extended with features for the
owner's own use cases. Build the requested features here and open pull requests against this fork, never against
`PaulWoitaschek/Voice` (see "Pull requests" above). Product scope in the `design`
skill reflects upstream's choices; when it conflicts with a feature the owner asked for, the owner's request wins.

## Commands

- Build: `./gradlew :app:assembleFreeDebug`. `freeDebug` is the local variant; `play` variants need
  `app/src/play/google-services.json` (Firebase, Crashlytics, Remote Config, Play review).
- Format, then lint: `./gradlew formatKotlin`, then `./gradlew lintKotlin` (ktlint + Compose rules, `.editorconfig`).
- Tests:
  - One class: `./gradlew :features:settings:testDebugUnitTest --tests "voice.features.settings.SettingsViewModelTest"`
  - One library module: `./gradlew :<module>:testDebugUnitTest`; app module: `./gradlew :app:testFreeDebugUnitTest`
  - Everything: `./gradlew voiceUnitTest`
- CI runs `./gradlew voiceUnitTest lintKotlin :app:assembleFreeDebug`.
- New module: `./scripts/new_module.main.kts :features:<name>`.
- Gradle runs out of memory by default; `scripts/gradle_bootstrap.sh` raises the heap in `~/.gradle/gradle.properties`.

## Architecture

Modules are listed in `settings.gradle.kts`; background in `docs/architecture.md`.

- `:app` → `:features:*` → `:navigation` → `:core:*`. `:app` holds the Metro DI graph (`app/src/main/kotlin/voice/app/di`)
  and `free`/`play` flavor wiring; keep it and `:navigation` thin.
- Features never depend on other features; core never depends on features. A contract with several implementations is
  split into `:api` plus implementation modules (`noop`, `firebase`, `play`, `free`, `impl`); only `:app` depends on
  implementations, often per flavor. `plugins/src/main/kotlin/checkModuleDependencies.kt` fails the build on violations.
- Convention plugins (`voice.library`, `voice.app`, `voice.compose`, `voice.ktlint`) live in the included build `plugins/`.
  Modules reference each other via type-safe accessors (`projects.core.data.api`) and libraries via `gradle/libs.versions.toml`.
- Stack: Compose + Material 3 Expressive, Metro DI, Navigation3 (`Destination` in `:navigation`, `NavEntryProvider` per
  feature), Media3/ExoPlayer, Room, DataStore, Kotlin Serialization, Coil.
- Never rename a DataStore file: that is where listeners' data lives.

## Skills

Project skills in `.agents/skills` (symlinked to `.claude/skills`) hold the detailed rules. Load the matching one before
working: `architecture` (modules, DI, navigation), `format-and-test` (test conventions, mockk/fakes, coroutines),
`design` (UI and feature decisions), `translate-strings` (`core/strings`), `snapshots` (render Compose previews),
`emulator` (run the app; debug builds can open any screen via the `debugBackStack` extra), `pull-request`,
`release-notes`, `sketching`, `firebase-app-distribution`.
