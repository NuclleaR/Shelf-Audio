# AGENTS.md

Voice is a modular Android audiobook player.

- Build the app: `./gradlew :app:assembleFreeDebug`.
- If installing a debug build fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, the installed app was signed with a different key. Never uninstall it to get around this, as that wipes the user's library. Ask the user instead.
- Only write code comments when absolutely necessary, to explain a non-obvious *why*. Don't restate what the code does or describe the change you made. This includes KDoc.
- Keep complexity low, as every line of code is maintained for years. Build the simplest version that solves the problem listeners actually have. If an edge case would add a lot of code, stored data or a migration, leave it out and name it in the pull request, or ask first.
- In the Play build, logs from `Logger.d` up go to Crashlytics. Never log names, titles, URIs, ids, server addresses, what the listener typed or whole objects. Log counts, positions and types instead. Pass exceptions as the throwable, don't put them in the message. Use `Logger.nonFatal` only for what should never happen.

## Pull requests

Shelf Audio is its own project, based on Voice. On GitHub this repository is still a fork of PaulWoitaschek/Voice, so pass `--repo NuclleaR/Shelf-Audio` to `gh`. Never open pull requests or issues in PaulWoitaschek/Voice.
