# Shelf Audio

[![CI](https://github.com/NuclleaR/Shelf-Audio/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/NuclleaR/Shelf-Audio/actions/workflows/ci.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE.md)

An audiobook player for Android. It plays your own audiobook files (M4B, MP3, M4A, OGG, OGA and OPUS), remembers where
you stopped, and has chapters, a sleep timer, bookmarks and playback speed.

Shelf Audio is based on [Voice](https://github.com/PaulWoitaschek/Voice) by Paul Woitaschek.

## Building

`./gradlew :app:assembleFreeDebug` builds a debug APK. Every push to `main` builds a signed release APK and AAB in
GitHub Actions.

## License

Shelf Audio is licensed under the [GNU GPLv3](LICENSE.md), like Voice.
