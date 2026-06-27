---
status: planned
confidence: high
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Vision

> OBSERVA is an offline-first AI vision assistant that gives blind and low-vision users continuous ambient awareness of their surroundings — then helps them act on it.

## The problem with existing AI vision assistants

Today's AI vision assistants (camera-to-cloud "describe this" apps) share a hidden assumption: **that the user can already aim the camera at the thing they care about.** For a sighted user that's trivial. For a blind user it is the entire problem. You cannot point a camera at the sign, the doorway, the spilled liquid, or the oncoming obstacle if you don't know it's there or where it is.

These tools also assume connectivity, send imagery of the user's private environment to the cloud, and add round-trip latency that makes them useless for anything time-sensitive or safety-relevant.

## How OBSERVA reverses the assumption

OBSERVA inverts the interaction model:

1. **Ambient awareness first.** A lightweight, always-on, on-device model continuously understands the scene and surfaces what matters (obstacles, people, text, hazards) without the user having to aim at anything. See [[ambient-awareness]] and [[two-tier-inference]].
2. **Guided interaction second.** Once the user knows *something* is there, OBSERVA helps them orient toward it — "text to your left," "step down ahead" — turning aiming from a prerequisite into a guided step. See [[spatial-guidance]].
3. **On-demand depth third.** When the user wants detail, a heavier on-demand tier reads text ([[ocr-mode]]) or answers questions about the scene ([[conversational-vision]]).

## Non-negotiable principles

- **Offline-first** — the safety core never requires the cloud. See [[offline-first-design]] and [[ADR-0001-offline-first]].
- **Privacy-first** — imagery stays on the device by default. See [[privacy-model]].
- **Accessibility-first** — designed for non-visual use from the ground up. See [[accessibility-principles]].
- **Safety-first** — never give confidently wrong guidance about hazards. See [[safety-principles]].
- Low latency, battery efficient, Android-native, on-device inference, airplane-mode capable.
- Plus: on-device **translation** and **navigation** assistance as core directions.

## Current Reality
A working CameraX preview + frame-analysis loop and permission flow exist; no model inference yet. The vision above is the target, not the current state. See [[system-overview]].

## Future Vision
A continuously-aware assistant that, over time, grows into a permissioned assistive AI layer — see [[ai-os-expansion]] — while keeping the offline safety core isolated from any online capability.
