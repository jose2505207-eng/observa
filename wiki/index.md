# Index

Complete map of the OBSERVA wiki. Every page should be listed here.

## Top level
- [[README]] — how to use this wiki.
- [[index]] — this page.
- [[log]] — chronological project log.

## Schema
- [[wiki-rules]] — metadata, linking, and maintenance rules.
- [[page-template]] — template for a standard page.
- [[source-template]] — template for a source/research note.
- [[decision-template]] — template for an ADR.

## Sources
- [[sources/README]] — how source material is organized (`raw/`, `processed/`).

## Product
- [[vision]] — why OBSERVA exists; the "blind users can't aim a camera" problem.
- [[user-personas]] — who we build for.
- [[core-use-cases]] — primary user journeys.
- [[accessibility-principles]] — non-negotiable accessibility rules.
- [[safety-principles]] — safety model and constraints.
- [[ai-os-expansion]] — long-term assistive AI OS vision.

## Architecture
- [[system-overview]] — the whole system at a glance.
- [[two-tier-inference]] — Tier 1 always-on vs Tier 2 on-demand.
- [[android-stack]] — Android/Compose/CameraX stack.
- [[executorch-qnn]] — on-device inference runtime and acceleration.
- [[offline-first-design]] — offline-first guarantees.
- [[privacy-model]] — data handling and privacy guarantees.
- [[performance-targets]] — latency, battery, memory targets.

## Features
- [[ambient-awareness]] — Tier 1 continuous scene understanding.
- [[spatial-guidance]] — guided interaction / aiming help.
- [[offline-navigation]] — GPS + compass bearing guidance and offline map packs (v2.5.0).
- [[offline-translation]] — on-device ML Kit translation + language-pack download (v2.5.0).
- [[ocr-mode]] — text reading.
- [[conversational-vision]] — ask-about-the-scene VLM.
- [[voice-command-layer]] — voice control.
- [[alarms-reminders-skill]] — alarms & reminders skill.
- [[email-reading-writing-skill]] — email skill.
- [[web-search-online-skill]] — online search skill.
- [[mcp-skill-system]] — permissioned skill framework.

## Engineering
- [[development-loop]] — how we work session to session.
- [[build-and-run]] — build, install, run, test commands.
- [[testing-plan]] — testing strategy.
- [[profiling-plan]] — performance/battery profiling.
- [[model-selection]] — model choices and tradeoffs.
- [[risks-and-mitigations]] — engineering risks.

## Demo
- [[airplane-mode-demo]] — the core offline demo.
- [[judge-script]] — what to say to judges.
- [[demo-checklist]] — pre-demo checklist.
- [[failure-recovery]] — what to do if something breaks live.

## Decisions (ADRs)
- [[ADR-0001-offline-first]] — offline-first is mandatory.
- [[ADR-0002-two-tier-architecture]] — two-tier inference.
- [[ADR-0003-skill-system-boundaries]] — skill system isolation.

## Roadmap
- [[hackathon-mvp]] — MVP scope.
- [[post-hackathon]] — near-term after the hackathon.
- [[long-term-ai-os]] — long-term direction.
