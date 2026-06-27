---
status: planned
confidence: medium
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Judge Script

> What to say to judges. Lead with the problem, prove offline, be truthful about status.

## 30-second hook
"Every AI vision app assumes a blind user can already aim the camera at the thing they care about. That assumption *is* the problem. OBSERVA flips it: continuous on-device awareness tells you what's around you first, then guides you to it — and it all runs offline, in airplane mode." (See [[vision]].)

## The demo beat
"Watch — I'm turning on airplane mode. No cloud now." Then run [[airplane-mode-demo]].

## What to emphasize
- **Offline-first / privacy-first:** imagery never leaves the device ([[privacy-model]], [[offline-first-design]]).
- **Accessibility-first:** designed for non-visual use ([[accessibility-principles]]).
- **Two-tier architecture:** always-on awareness + on-demand depth, where Tier 2 never blocks Tier 1 ([[two-tier-inference]]).

## Truthfulness (important)
State current status plainly: the always-on offline camera/analysis loop is real today; on-device model inference is the active build target. Do not claim inference works if it doesn't (see [[wiki-rules]]). Judges trust honest teams.

## Vision close
"From here, OBSERVA grows into a permissioned assistive AI layer — translation, navigation, everyday skills — while the offline safety core stays isolated." ([[ai-os-expansion]])

## Related
- [[demo-checklist]] · [[failure-recovery]]
