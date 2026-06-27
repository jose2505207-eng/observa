# Manual test — foreground service (v1.6)

Device: Galaxy S25 Ultra, Android 16, **Airplane Mode**. Grant Camera, Microphone, Notifications.

## What v1.6 adds (honest scope)
- A real **foreground service** (`AmbientAwarenessService`, type `camera`) that keeps OBSERVA
  foregrounded with a persistent, honest notification and accessible **Stop / Mute / Repeat**
  actions routed to the controller via `ServiceBridge`.
- **Adaptive duty cycle** from real battery + thermal state (`BatteryThermalPolicy`): the analysis
  loop slows under heat / low-battery and reduces cue chatter; surfaced in the dashboard **Service**
  row and `serviceStatus`.
- Pure, unit-tested logic: `ServiceStateReducer` (lifecycle/degrade transitions), notification text,
  permission education copy.

> Not yet implemented (documented limitation): true **screen-off background camera capture**. The
> camera + inference still run while the Activity is in the foreground; the service provides the
> reliable foreground presence, controls, and status. Closing this is a v1.6+ follow-up.

## Checks
| # | Check | Pass criteria | Status |
|---|---|---|---|
| 1 | Build / unit tests | assembleDebug + 92 tests pass | **PASS** |
| 2 | No INTERNET permission | `aapt2 dump permissions` shows none | **PASS** |
| 3 | Airplane-mode launch | app runs, no crash | **PASS** |
| 4 | Camera preview | visible, frames increment | **PASS** |
| 5 | Foreground service runs | `dumpsys activity services` → `isForeground=true types=0x40` | **PASS** |
| 6 | Notification appears | "OBSERVA running offline… Observing. No network used." in shade | **PASS** (screenshot) |
| 7 | Notification actions | Stop / Mute speech / Repeat last present (actions=3) | **PASS** (dumpsys) |
| 8 | Stop action | tapping Stop stops observing + stops service | needs manual tap |
| 9 | Mute action | tapping Mute mutes speech (safety cues continue) | needs manual tap |
| 10 | Repeat action | tapping Repeat replays last message | needs manual tap |
| 11 | Degraded honesty (camera) | revoke camera → notification/status explains, no crash | needs manual |
| 12 | Battery policy | discharge below 15% → "low battery: reduced rate" (unit-tested; device was charging) | unit-verified |
| 13 | Thermal policy | under thermal warning → reduced rate (unit-tested; not forced on device) | unit-verified |
| 14 | Screen-off | **documented limitation**: capture pauses with the Activity; service + notification persist | documented |

## How to verify on device
```
adb shell dumpsys activity services com.observa.app | grep -i foreground
adb shell cmd statusbar expand-notifications   # see the OBSERVA notification + actions
adb shell pm revoke com.observa.app android.permission.CAMERA   # then observe degraded copy
```

## Result log
| # | Result | Notes |
|---|---|---|
| 1–14 | | |
