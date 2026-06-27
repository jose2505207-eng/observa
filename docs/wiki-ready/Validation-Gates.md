# Validation Gates

Every change passes the relevant gate before merge. `main` must always stay demo-safe ([[GitHub Workflow]]).

## Gate A: Build Gate
```bash
./gradlew assembleDebug
```
**Pass:** build succeeds · no new critical warnings · APK generated.

## Gate B: Install Gate
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
**Pass:** install succeeds · app launches.

## Gate C: Camera Gate
```bash
adb shell monkey -p com.observa.app 1
```
**Pass:** camera permission flow works · preview starts · frame count increases.

## Gate D: Alert Gate
**Pass:** app produces at least one spoken, visual, or haptic alert · alert is understandable · alert does not repeat endlessly (REQ-004, REQ-005).

## Gate E: Offline Gate
**Pass:** Airplane Mode enabled · core demo still works · no cloud dependency required (REQ-001, REQ-008).

## Gate F: Performance Gate
**Pass:** FPS visible · frame count visible · latency visible if an inference path exists · backend status visible (REQ-006, REQ-007). Latency/backend may read "N/A" only if there is genuinely no inference path — labeled truthfully.

## Gate G: Accessibility Gate
**Pass:** TalkBack labels exist for major controls · text is readable · app usable with minimal touch · alerts spoken clearly (REQ-009).

## Gate H: Documentation Gate
**Pass:** README setup current · wiki reflects current architecture · demo script exists · known limitations documented honestly.

## Gate I: Git Gate
**Pass:** working tree clean or intentionally staged · commit message references a requirement or wiki page · branch pushed · no secrets committed. Enforced by [[Auto Push Skill]].

---

### Gate → Requirement quick map
| Gate | Covers |
|------|--------|
| A | REQ-015 |
| B | REQ-015 |
| C | REQ-002 |
| D | REQ-003, REQ-004, REQ-005 |
| E | REQ-001, REQ-008 |
| F | REQ-006, REQ-007, REQ-014 |
| G | REQ-009, REQ-012 |
| H | all docs |
| I | repo safety |
