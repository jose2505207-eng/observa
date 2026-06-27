# OBSERVA Wiki

This wiki is the **single source of truth** for the OBSERVA project. It is the project's long-term memory: vision, architecture, features, engineering plans, decisions, demo planning, roadmap, and a running log. There is intentionally no `CLAUDE_PROJECT.md` and no second tracker.

This is an Obsidian-compatible markdown vault. Pages link to each other with `[[wiki-links]]`.

## Start here

- [[index]] — the full page index, grouped by area.
- [[log]] — chronological project log. Read the recent entries before starting work.
- [[vision]] — what OBSERVA is and why it exists.
- [[system-overview]] — how the system is built.

## How to use this wiki

1. **Read before changing.** Before modifying the repo, read this README, [[index]], the pages relevant to your task, and recent [[log]] entries.
2. **Keep it truthful.** Every page distinguishes **Current Reality** (proven by the repository) from **Future Vision**. A capability is only `status: current` if the code demonstrates it. Otherwise it is `planned` or `research`.
3. **Keep it in sync.** When you change code, update the affected pages, update [[index]] if you add pages, and append an entry to [[log]].

## Conventions

- Page metadata, templates, and linking rules live in [[wiki-rules]].
- Use the templates in `schema/` when creating new pages: [[page-template]], [[source-template]], [[decision-template]].
- Source material (research, datasheets, transcripts) goes in `sources/` — see [[sources/README]].

## What OBSERVA is

OBSERVA is an **offline-first, privacy-first, accessibility-first AI vision assistant** for blind and low-vision users on Android. The safety-critical core runs entirely on-device and works in airplane mode. See [[vision]] and [[accessibility-principles]].
