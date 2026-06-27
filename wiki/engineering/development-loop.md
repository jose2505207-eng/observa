---
status: current
confidence: high
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Development Loop

> How a Claude Code (or human) session should operate in this repo.

## Every session
1. Read `CLAUDE.md`.
2. Read [[README]], [[index]], and the wiki pages relevant to the task.
3. Read recent entries in [[log]].
4. Do the work, keeping changes surgical (see `CLAUDE.md` guidelines).
5. **Update the wiki in the same change:** affected pages, statuses, [[index]] if pages were added, and a new [[log]] entry.
6. Validate markdown and check for broken `[[links]]`.
7. `git status`, review the diff, commit with a meaningful message, push.

## Truthfulness gate
Before marking anything `status: current` or writing "Current Reality," confirm the repository actually demonstrates it. Otherwise use `planned` or `research`. This is enforced by [[wiki-rules]].

## Definition of done for a feature
- Code works and is verified ([[testing-plan]]).
- Wiki updated to match ([[wiki-rules]]).
- Log entry appended.

## Related
- [[build-and-run]] · [[testing-plan]]
