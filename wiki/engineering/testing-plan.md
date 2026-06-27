---
status: planned
confidence: medium
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Testing Plan

> Strategy for keeping OBSERVA correct as it grows. Mostly planned; the scaffolding exists.

## Current Reality
- Default Android test scaffolding is present: `ExampleUnitTest` (JVM, `app/src/test/`) and `ExampleInstrumentedTest` (`app/src/androidTest/`).
- No project-specific tests yet.

## Planned coverage
- **Unit (JVM):** frame-to-tensor conversion, event prioritization/debounce logic, intent routing, skill boundary enforcement.
- **Instrumented:** camera permission flow, analyzer loop liveness, Compose UI semantics for accessibility ([[accessibility-principles]]).
- **Accessibility checks:** TalkBack-relevant `contentDescription` coverage; screen-reader navigation smoke tests.
- **Safety regression:** when hazard logic exists, lock its behavior with tests ([[safety-principles]]).

## Approach
- Follow the `CLAUDE.md` goal-driven rule: for a bug, write a failing test first; for a feature, define the check up front.

## Related
- [[build-and-run]] · [[profiling-plan]]
