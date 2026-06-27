# Parallel Work Plan

Workstreams that can run at the same time. Each can progress against a **mocked interface** so no one is blocked. Owners map to [[Team Roles]]; validation maps to [[Validation Gates]].

## Workstream A: Android Camera + UI
- **Owner:** Android Lead
- **Parallel with:** AI Runtime, Docs, Demo
- **Files:** `app/src/main/java`, `app/src/main/res`
- **Output:** stable preview, permission flow, UI shell
- **Validation:** Camera Gate (C), Build Gate (A)

## Workstream B: AI Runtime
- **Owner:** AI Runtime Lead
- **Parallel with:** Camera (if interface is mocked)
- **Files:** `app/src/main/java`, `app/src/main/assets`, model/export scripts if present
- **Output:** model loading, inference function, backend status
- **Validation:** AI Runtime Loop, Performance Gate (F)

## Workstream C: Hazard Engine
- **Owner:** Perception Lead
- **Parallel with:** AI Runtime (using fake detections)
- **Output:** detection-to-alert logic, confidence filtering, duplicate suppression
- **Validation:** Alert Gate (D)

## Workstream D: Accessibility Output
- **Owner:** Accessibility Lead
- **Parallel with:** Hazard Engine (using fake alerts)
- **Output:** speech, haptics, TalkBack labels, high-contrast UI
- **Validation:** Accessibility Gate (G)

## Workstream E: Performance Dashboard
- **Owner:** Performance Lead
- **Parallel with:** Camera and AI Runtime
- **Output:** FPS, frame count, latency, backend display
- **Validation:** Performance Gate (F)

## Workstream F: Demo and Story
- **Owner:** Demo Lead
- **Parallel with:** all streams
- **Output:** script, Demo Mode, backup video
- **Validation:** Demo Loop

## Workstream G: Docs and Wiki
- **Owner:** Documentation Lead
- **Parallel with:** all streams
- **Output:** README, wiki pages, troubleshooting
- **Validation:** Documentation Gate (H)

## Integration rhythm (every ~90 minutes during the hackathon)
1. Pull latest.
2. Run build (Gate A).
3. Run Camera Gate (C) if a phone is available.
4. Report status in the team channel.
5. Update the wiki if assumptions changed.
6. Push small working commits ([[Auto Push Skill]]).

## Interface contracts to mock
- **Frame seam:** `handleFrame(imageProxy)` → tensor (A↔B).
- **Detections:** AI Runtime → Perception (B↔C) — a list of `{label, confidence, bbox}`.
- **Alert events:** Perception → Accessibility (C↔D) — `{message, direction, severity}`.
Agree these shapes early so streams stay unblocked.
