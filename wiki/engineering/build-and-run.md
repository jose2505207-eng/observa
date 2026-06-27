---
status: current
confidence: high
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Build & Run

> Verified commands for building, running, and testing OBSERVA. Use the Gradle wrapper.

## Commands
- Build debug APK: `./gradlew assembleDebug`
- Install on a connected device/emulator: `./gradlew installDebug`
- Run JVM unit tests (`app/src/test/`): `./gradlew test`
- Run a single unit test class: `./gradlew testDebugUnitTest --tests "com.observa.app.ExampleUnitTest"`
- Run instrumented tests (`app/src/androidTest/`, needs device/emulator): `./gradlew connectedAndroidTest`
- Lint: `./gradlew lint`

## Project facts (Current Reality)
- Kotlin 2.2.10, AGP 9.2.1, Gradle Kotlin DSL.
- `minSdk 26`, `targetSdk/compileSdk 36`, Java 11.
- Dependencies in the version catalog `gradle/libs.versions.toml` (referenced as `libs.*`).
- ExecuTorch linked from `app/libs/executorch.aar` (local AAR; not yet called — see [[executorch-qnn]]).
- R8 keep rules go in `app/src/main/keepRules/`.

## Running the app
- Requires a device/emulator with a camera; the app requests camera permission at first launch and shows a live frame counter (proof the analyzer loop runs). See [[ambient-awareness]].
- `local.properties` must point to a valid Android SDK (`sdk.dir`).

## Related
- [[android-stack]] · [[development-loop]] · [[testing-plan]]
