# GPS Orientation Lite — demo

Honest: heading/bearing/distance guidance, not turn-by-turn maps. Fully offline (satellite GPS +
compass). Hazards always interrupt orientation.

## Setup
```
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm grant com.observa.app android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.observa.app android.permission.ACCESS_COARSE_LOCATION
```
Go **outdoors / near a window** for a GPS fix. Airplane Mode is fine (GPS is satellite, not network).

## Demo
1. Launch OBSERVA. Confirm the detector runs (dashboard "AI model: loaded — CPU").
2. TalkBack on → focus the **Available actions** node → actions menu → **Start orientation**.
3. Hear the opening: *"Orientation on. Acquiring GPS toward the demo destination."*
4. Once a fix is acquired, hear guidance every ~4 s in the six-way vocabulary, e.g.
   *"Destination ahead-left, 40 meters. Turn slightly left."* / *"Destination ahead, 25 meters."* /
   *"Destination behind. Turn around safely."*
5. Rotate the phone — guidance updates with your heading.
6. **Repeat orientation** re-speaks the latest guidance; **Stop orientation** ends it.
7. Point the camera at a person while orientation runs → the **hazard interrupts** orientation
   (HAZARD > NAVIGATION, plus the `NavigationSafetyArbiter` hold window), proving safety priority.

## Honest states to expect
- No fix yet / indoors: *"GPS signal weak. Acquiring location..."* (confidence: weak GPS).
- Compass uncalibrated: *"Compass unstable. Move the phone in a figure eight."*
- Permission denied: *"Orientation needs location permission to guide you."*

## What it is not
No street routing, no map packs, no live re-routing. It orients you toward a coordinate.
