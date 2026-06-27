# Project Log

Newest entries first. Append an entry whenever you make a meaningful change to the code or wiki. Keep entries short and factual.

---

## 2026-06-27 — Wiki bootstrap

- Adopted the **LLM Wiki** philosophy: `wiki/` is now the single source of truth. Removed the placeholder `CLAUDE_PROJECT.md` and repointed `CLAUDE.md` to the wiki.
- Created the full wiki structure (product, architecture, features, engineering, demo, decisions, roadmap) plus schema templates.
- Recorded **Current Reality** baseline from the repository:
  - Single-module Jetpack Compose app (`com.observa.app`), single `MainActivity`.
  - CameraX `Preview` + `ImageAnalysis` loop is wired and running; frames are counted on a background executor (`STRATEGY_KEEP_ONLY_LATEST`, `YUV_420_888`). See [[ambient-awareness]].
  - Runtime camera-permission flow with a fallback screen.
  - ExecuTorch ships as a local AAR (`app/libs/executorch.aar`) but is **not yet invoked** — no inference code exists. See [[executorch-qnn]].
  - Manifest declares audio + foreground-service (camera/mic) permissions; no foreground service is implemented yet.
- Authored ADRs: [[ADR-0001-offline-first]], [[ADR-0002-two-tier-architecture]], [[ADR-0003-skill-system-boundaries]].
- Added repository `README.md` pointing to the wiki.

**Status:** camera/analyzer scaffold is real; everything else (inference, OCR, VLM, skills, navigation, translation, voice) is `planned` or `research`.
