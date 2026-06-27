# Team Roles

Roles are designed so people work in parallel without blocking each other. Each role lists inputs, outputs, files likely touched, definition of done (DoD), and a handoff checklist. See [[Parallel Work Plan]] for the matching workstreams and [[Skills Matrix]] for skills.

---

## 1. Product Captain
**Responsibilities:** own the demo story; keep scope aligned to hackathon scoring; approve requirement changes; keep decisions user-centered.
- **Inputs:** scoring rubric, user need, team status.
- **Outputs:** prioritized scope, approved [[Product Requirements]], demo narrative direction.
- **Files likely touched:** wiki pages (Home, Product Requirements, Demo Plan).
- **DoD:** scope is current, requirements have owners, demo story is one sentence everyone can repeat.
- **Handoff:** confirm each P0 has an owner and a validation gate.

## 2. Android Lead
**Responsibilities:** CameraX pipeline; `MainActivity` / Compose UI; permissions; app lifecycle; build reliability.
- **Inputs:** requirements (REQ-001/008/009/015), device.
- **Outputs:** stable preview, permission flow, UI shell, reliable build.
- **Files likely touched:** `app/src/main/java/com/observa/app/`, `app/src/main/res/`, `app/build.gradle.kts`, `AndroidManifest.xml`.
- **DoD:** Gate A (Build) + Gate C (Camera) pass on a device.
- **Handoff:** documented build/run steps; stable frame seam (`handleFrame`) for AI Runtime.

## 3. AI Runtime Lead
**Responsibilities:** ExecuTorch integration; QNN backend investigation; model loading; inference performance; CPU fallback.
- **Inputs:** target model + input shape, frames from the seam, `app/libs/executorch.aar`.
- **Outputs:** inference function, backend status string, CPU fallback, truthful docs.
- **Files likely touched:** `app/src/main/java/...`, `app/src/main/assets/` (models), export scripts if added.
- **DoD:** real-frame inference runs; backend truthfully reported (REQ-007); Gate F latency visible.
- **Handoff:** documented model format/ABI; detection output contract for Perception.

## 4. Perception Lead
**Responsibilities:** detection parsing; hazard priority logic; tracking; confidence filtering; scene-state memory.
- **Inputs:** raw detections from AI Runtime (or mocks).
- **Outputs:** prioritized, de-duplicated hazard events with direction.
- **Files likely touched:** `app/src/main/java/...` (reasoning module).
- **DoD:** Gate D (Alert) passes; safety-relevant alerts rank first (REQ-003); no spam (REQ-005).
- **Handoff:** stable alert event schema for Accessibility output.

## 5. Accessibility Lead
**Responsibilities:** TalkBack labels; large text; high contrast; speech policy; haptic patterns; blind-user demo flow.
- **Inputs:** alert events (or mocks), UI shell.
- **Outputs:** TTS output, haptics, accessible UI, eyes-free flow.
- **Files likely touched:** `app/src/main/java/...`, `app/src/main/res/values/` (themes/strings/colors).
- **DoD:** Gate G (Accessibility) passes; usable without looking at screen.
- **Handoff:** speech/haptic policy documented for Demo Lead.

## 6. Performance Lead
**Responsibilities:** FPS; latency; memory; battery notes; benchmark dashboard; dropped-frame tracking.
- **Inputs:** running pipeline.
- **Outputs:** on-screen dashboard (REQ-006), benchmark table (REQ-014).
- **Files likely touched:** `app/src/main/java/...` (instrumentation), wiki benchmark table.
- **DoD:** Gate F passes; FPS/frame-count/latency/backend/dropped visible (or honestly N/A).
- **Handoff:** baseline numbers recorded for the demo.

## 7. Demo Lead
**Responsibilities:** Demo Mode; judge script; fake route/map if needed; Airplane Mode proof; backup video/screenshots.
- **Inputs:** working features, [[Demo Plan]].
- **Outputs:** deterministic demo path, rehearsed script, backups.
- **Files likely touched:** demo scaffolding, wiki Demo Plan.
- **DoD:** Demo Loop runs end-to-end in Airplane Mode; backups exist.
- **Handoff:** final script + backup assets to Product Captain.

## 8. Documentation Lead
**Responsibilities:** README; wiki; architecture diagrams; setup; troubleshooting; final submission narrative.
- **Inputs:** changes from all roles.
- **Outputs:** current README + wiki; [[Troubleshooting]]; submission write-up.
- **Files likely touched:** `README.md`, `docs/wiki-ready/`, wiki repo.
- **DoD:** Gate H passes; wiki matches reality; limitations documented honestly.
- **Handoff:** publish wiki (see docs/wiki-ready README) once initialized.

## 9. GitHub Release Lead
**Responsibilities:** branch hygiene; PR reviews; auto-push skill; tags/releases; final commit verification.
- **Inputs:** branches, PRs.
- **Outputs:** clean history, validated merges, working [[Auto Push Skill]].
- **Files likely touched:** `.claude/skills/git-auto-push/SKILL.md`, `.github/` if added.
- **DoD:** Gate I passes; `main` is demo-safe; no secrets committed.
- **Handoff:** final tag + verified build to Product Captain.
