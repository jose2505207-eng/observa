# Product Requirements

Structured requirements with stable IDs. Reference these IDs in branches, commits, and PRs. Status is honest: `Done`, `In progress`, `Planned`, or `Needs verification`.

> Repo reality check (2026-06-27): only the CameraX preview + frame-count loop and permission flow are implemented. Inference, alerts, dashboard metrics beyond frame count, OCR, voice, and haptics are not yet built.

---

### REQ-001: Offline Operation
**Priority:** P0 · **Owner Role:** Android Lead / AI Runtime Lead · **Status:** Partially Done (no network code exists; alerts/inference pending)
**Description:** OBSERVA must operate without cloud dependency for core demo features.
**Acceptance Criteria:**
- App launches in Airplane Mode.
- Camera preview runs.
- Ambient detection loop runs.
- Speech/haptic alerts work.
- No network call is required for the core demo.

### REQ-002: Ambient Awareness
**Priority:** P0 · **Owner Role:** Perception Lead / AI Runtime Lead · **Status:** Partial (frame loop runs; no scene understanding yet)
**Description:** The app must continuously process camera frames and detect meaningful scene changes or hazards.
**Acceptance Criteria:**
- Frames processed continuously without blocking UI.
- Meaningful scene change / hazard signal produced from frames.

### REQ-003: Hazard Prioritization
**Priority:** P0 · **Owner Role:** Perception Lead · **Status:** Planned
**Description:** The system must prioritize safety-relevant alerts over generic object labels.
**Examples:** Obstacle ahead · Person approaching · Doorway left · Stairs detected · Text detected · Crosswalk/route marker if available.
**Acceptance Criteria:**
- Safety-relevant detections rank above generic labels in the alert queue.

### REQ-004: Directional Feedback
**Priority:** P0 · **Owner Role:** Accessibility Lead / Perception Lead · **Status:** Planned
**Description:** Alerts must provide directional guidance via spatial audio, stereo panning, haptics, or clear spoken direction.
**Acceptance Criteria:**
- Alert conveys a direction (left/center/right/up/down) a non-sighted user can act on.

### REQ-005: Non-Spammy Alerts
**Priority:** P0 · **Owner Role:** Perception Lead / Accessibility Lead · **Status:** Planned
**Description:** The app must suppress repeated alerts and only speak when the scene meaningfully changes.
**Acceptance Criteria:**
- Identical/near-identical alerts are debounced; no endless repetition.

### REQ-006: Performance Dashboard
**Priority:** P0 · **Owner Role:** Performance Lead · **Status:** Partial (frame count shown; FPS/latency/backend/dropped pending)
**Description:** The app must expose FPS, frame count, inference latency, backend mode, and dropped frame count.
**Acceptance Criteria:**
- All five metrics visible on-screen (latency/backend may read "N/A" until inference exists, labeled truthfully).

### REQ-007: ExecuTorch / QNN Visibility
**Priority:** P0 · **Owner Role:** AI Runtime Lead · **Status:** Needs verification (AAR bundled, not invoked)
**Description:** If ExecuTorch and Qualcomm QNN integration exists or is in progress, the UI and docs must show the current backend state truthfully.
**Acceptance Criteria:**
- UI shows actual backend (e.g., `CPU`, `QNN`, `ExecuTorch-XNNPACK`, or `none`) without overstating.

### REQ-008: Privacy Proof
**Priority:** P0 · **Owner Role:** Demo Lead / Android Lead · **Status:** Partial (no network code today)
**Description:** The demo must show that OBSERVA runs locally without uploading camera data.
**Acceptance Criteria:**
- Airplane Mode demo + a visible "local only / no upload" indicator or stated guarantee.

### REQ-009: Accessibility UI
**Priority:** P0 · **Owner Role:** Accessibility Lead · **Status:** Partial (contentDescription on preview/permission only)
**Description:** UI must be usable with large text, high contrast, TalkBack labels, and minimal touch interaction.
**Acceptance Criteria:**
- Major controls have TalkBack labels; large text + high contrast; usable with minimal touch.

### REQ-010: Demo Mode
**Priority:** P1 · **Owner Role:** Demo Lead · **Status:** Planned
**Description:** Deterministic demo path judges can understand quickly.
**Acceptance Criteria:**
- A repeatable scripted path that reliably produces representative alerts.

### REQ-011: OCR On Demand
**Priority:** P1 · **Owner Role:** AI Runtime Lead / Perception Lead · **Status:** Planned
**Description:** The user can request text reading when text is detected or on demand.
**Acceptance Criteria:**
- A trigger reads detected text aloud.

### REQ-012: Voice Interaction
**Priority:** P1 · **Owner Role:** Accessibility Lead · **Status:** Planned
**Description:** The user can trigger core actions by voice or a very simple control.
**Acceptance Criteria:**
- At least one core action reachable hands-free / via a single simple control.

### REQ-013: Haptic Feedback
**Priority:** P1 · **Owner Role:** Accessibility Lead · **Status:** Planned
**Description:** Vibration patterns for directional or severity-based warnings.
**Acceptance Criteria:**
- Distinct vibration patterns map to direction/severity.

### REQ-014: Benchmark Logging
**Priority:** P1 · **Owner Role:** Performance Lead · **Status:** Planned
**Description:** Save or display latency, FPS, memory, battery/thermal notes where feasible.
**Acceptance Criteria:**
- Benchmark values captured to screen or log for the demo.

### REQ-015: Clean Build
**Priority:** P0 · **Owner Role:** Android Lead / Documentation Lead · **Status:** Needs verification (run Gate A on a clean machine)
**Description:** A fresh developer can build the project using documented commands.
**Acceptance Criteria:**
- `./gradlew assembleDebug` succeeds from a clean clone with documented setup.

---

## Requirement Ownership Map

| REQ | Title | Priority | Primary Owner Role |
|-----|-------|----------|--------------------|
| REQ-001 | Offline Operation | P0 | Android Lead / AI Runtime Lead |
| REQ-002 | Ambient Awareness | P0 | Perception Lead / AI Runtime Lead |
| REQ-003 | Hazard Prioritization | P0 | Perception Lead |
| REQ-004 | Directional Feedback | P0 | Accessibility Lead / Perception Lead |
| REQ-005 | Non-Spammy Alerts | P0 | Perception Lead / Accessibility Lead |
| REQ-006 | Performance Dashboard | P0 | Performance Lead |
| REQ-007 | ExecuTorch / QNN Visibility | P0 | AI Runtime Lead |
| REQ-008 | Privacy Proof | P0 | Demo Lead / Android Lead |
| REQ-009 | Accessibility UI | P0 | Accessibility Lead |
| REQ-010 | Demo Mode | P1 | Demo Lead |
| REQ-011 | OCR On Demand | P1 | AI Runtime Lead / Perception Lead |
| REQ-012 | Voice Interaction | P1 | Accessibility Lead |
| REQ-013 | Haptic Feedback | P1 | Accessibility Lead |
| REQ-014 | Benchmark Logging | P1 | Performance Lead |
| REQ-015 | Clean Build | P0 | Android Lead / Documentation Lead |

See [[Team Roles]] for what each role owns and [[Validation Gates]] for how each requirement is verified.
