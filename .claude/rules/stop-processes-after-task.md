<!-- ai-brain: added=2026-10-28 -->

# Stop Gradle, the emulator and helper processes when a task is done

When a task's work cycle is finished (code written, checked on the emulator, bugs fixed), stop everything you started for it, in the same turn as the final report: `./gradlew --stop` (the Kotlin compile daemon exits with it), `.agents/skills/emulator/scripts/emulator.py stop`, and any other background process or terminal tab you opened. Only stop what you started, never the owner's own emulators, devices or apps.

**Why:** idle Gradle and Kotlin daemons held about 5 GiB and an agent emulator about 7 GiB of RAM after tasks were over, until the owner found them in the system monitor.

**How to apply:** make "stop Gradle, the emulator and helpers" the last step of every plan, and say in the report that they are stopped.
