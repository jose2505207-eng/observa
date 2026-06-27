# GitHub Workflow

Workflow for parallel development. `main` is sacred: always demo-safe.

## Branches
- `main` — always demo-safe. Only merge validated work.
- `develop` — optional integration branch if the team chooses to use one.
- `feature/*` — new functionality (e.g., `feature/REQ-006-perf-dashboard`).
- `fix/*` — bug fixes.
- `docs/*` — wiki / README changes.
- `demo/*` — demo-specific scripts or polish.

## Commit format
`AREA-ID: concise description`

Examples:
```
REQ-001: add offline mode status indicator
AI-002: add inference latency metric
DOC-003: update demo script
FIX-004: prevent repeated speech alerts
```

## PR checklist
- [ ] Requirement ID linked.
- [ ] Build passes (Gate A).
- [ ] Relevant [[Validation Gates]] pass.
- [ ] Wiki updated if behavior changed.
- [ ] Screenshots/video attached for UI changes.
- [ ] No secrets.
- [ ] No false claims about NPU/QNN/backend (see [[Technical Architecture]]).

## Merge policy
- During crunch, small direct commits are acceptable **only if** auto-push validation passes ([[Auto Push Skill]]).
- `main` must remain demo-safe at all times.
- Risky experiments stay on branches.

## Safe pushing
Use the [[Auto Push Skill]] (`git-auto-push`). It refuses on build failure, detected secrets, or unintended direct pushes to `main`, and it auto-creates an `auto/<name>` branch when you're on `main` without explicit approval.
