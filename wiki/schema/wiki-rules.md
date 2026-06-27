---
status: current
confidence: high
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Wiki Rules

Rules that govern every page in this wiki. The wiki is the project's only memory; treat it as the operating manual.

## 1. Single source of truth
- The wiki is the only project memory. Do not create a `CLAUDE_PROJECT.md` or any parallel tracker.
- If information matters to the project, it lives here.

## 2. Page metadata
Every applicable page begins with YAML frontmatter:

```yaml
status: current | planned | research | risk | decision
confidence: high | medium | low
last_updated: YYYY-MM-DD
owner:
```

- `status` meanings:
  - `current` — proven by the repository **today**.
  - `planned` — intended, not yet built.
  - `research` — being investigated; feasibility not confirmed.
  - `risk` — documents a risk.
  - `decision` — an ADR.

## 3. Current Reality vs Future Vision
- Every page must clearly separate **Current Reality** from **Future Vision**. Never blend them in the same paragraph.
- Only label something `current` / "Current" if the code in this repo demonstrates it.

## 4. Engineering truthfulness
- Never invent implementation details.
- For ExecuTorch, Qualcomm QNN, Android APIs, hardware acceleration, and model compatibility, always tag a claim as **Current**, **Planned**, or **Research**.
- When uncertain, say so explicitly and lower the page `confidence`.

## 5. Linking
- Use Obsidian `[[wiki-links]]` to connect related pages. Link generously.
- Link to a page by its filename without extension, e.g. `[[two-tier-inference]]`.

## 6. Maintenance
When code changes, in the same change:
- update affected pages,
- update implementation status,
- update [[index]] if pages were added/removed,
- append an entry to [[log]].

The code and wiki must never diverge.
