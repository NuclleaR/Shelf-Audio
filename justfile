default:
    @just --list

# Release APK, signed when signing/signing.properties exists
build:
    ./gradlew :app:assembleFreeRelease

# Debug APK
debug:
    ./gradlew :app:assembleFreeDebug

# Remove Gradle build outputs
clean:
    ./gradlew clean

# Install the debug app on the emulator from the emulator skill, in a window, and open it
run:
    #!/usr/bin/env bash
    set -euo pipefail
    sdk="${ANDROID_HOME:-$(sed -n 's/^sdk.dir=//p' local.properties)}"
    export PATH="$sdk/platform-tools:$sdk/emulator:$PATH"
    serial=$(.agents/skills/emulator/scripts/emulator.py start --name just --window | tee /dev/stderr | sed -n 's/^ANDROID_SERIAL=//p')
    export ANDROID_SERIAL="$serial"
    ./gradlew :app:installFreeDebug
    .agents/skills/emulator/scripts/ui.py open BookOverview
