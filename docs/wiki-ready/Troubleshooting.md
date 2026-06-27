# Troubleshooting

Practical fixes for common failures. Add a new entry whenever a bug was confusing (see [[Development Loops]] Loop 6).

## Gradle build fails
Check:
- Java version (project targets Java 11; use a compatible JDK).
- Android Gradle Plugin version (AGP 9.2.1).
- Android SDK installed; `compileSdk/targetSdk 36` available.
- `local.properties` has a valid `sdk.dir`.
- Gradle cache — try `./gradlew --stop` then rebuild; as a last resort clear `.gradle/`.

## ADB does not see the phone
```bash
adb devices
adb kill-server
adb start-server
```
Also: enable USB debugging; accept the RSA prompt on the device.

## App installs but does not launch
```bash
adb shell monkey -p com.observa.app 1
```
Check Logcat for the stack trace: `adb logcat | grep -i observa`.

## Camera preview works but no AI alerts
Check:
- Frame count (is the loop alive?).
- Model loaded flag.
- Backend status.
- Detection parser output.
- Alert threshold.
- Speech muted / volume.
> Note: as of now there is no inference or alert path implemented — alerts are expected to be absent until REQ-002/003/004 land.

## Speech does not work
Check:
- `TextToSpeech` initialization (success callback).
- Device volume / media stream.
- TTS language data installed.
- Audio focus.

## Haptics do not work
Check:
- Vibrator permission/API (`VIBRATE` is declared).
- Device haptics enabled in system settings.
- Android version compatibility (`VibrationEffect` on API 26+; project minSdk is 26).

## QNN / ExecuTorch does not work
Check:
- Native libraries included (ABI: `arm64-v8a`).
- ABI compatibility with the device/emulator.
- Model format (`.pte`).
- Backend availability at runtime.
- CPU fallback path engaged and labeled (see [[Technical Architecture]]).
> Current status: `app/libs/executorch.aar` is bundled but not yet invoked.

## Wiki changes not showing
Check:
- GitHub Wiki repo actually pushed.
- Correct remote (`observa.wiki.git`).
- Correct branch (wiki default is usually `master`).
- Page filename format (spaces → hyphens, e.g. `Team-Roles.md`).
- Browser cache.
> The GitHub Wiki must be **initialized once** (create any page via the GitHub UI) before `observa.wiki.git` exists to push to. See `docs/wiki-ready/README.md`.
