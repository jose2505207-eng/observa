# docs/wiki-ready — GitHub Wiki source

These markdown files are the **OBSERVA team wiki**, formatted for the GitHub Wiki (filenames use hyphens; links use `[[Page Title]]`). The GitHub Wiki git repo (`observa.wiki.git`) did **not exist** at authoring time, so the pages are staged here and version-controlled with the code.

## Pages
`Home`, `Product-Requirements`, `Technical-Architecture`, `Team-Roles`, `Skills-Matrix`, `Development-Loops`, `Validation-Gates`, `Demo-Plan`, `GitHub-Workflow`, `Auto-Push-Skill`, `Parallel-Work-Plan`, `Troubleshooting`, `Hackathon-Scoring-Strategy`, plus `_Sidebar` (nav).

## How to publish to the GitHub Wiki (one-time init required)

GitHub creates the wiki git repo only after the **first page** is created through the web UI.

1. Go to `https://github.com/jose2505207-eng/observa/wiki` and click **Create the first page**; save anything (e.g. an empty Home).
2. Then publish all pages from this folder:

```bash
git clone https://github.com/jose2505207-eng/observa.wiki.git
cp docs/wiki-ready/*.md observa.wiki/
cd observa.wiki
git add .
git commit -m "DOC-WIKI: add team operating system and validation gates"
git push origin master || git push origin main
```

3. Verify at `https://github.com/jose2505207-eng/observa/wiki`.

Keep these files and the published wiki in sync: edit here, then re-copy and push.
