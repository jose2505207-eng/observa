---
name: git-auto-push
description: Validate, commit, and push completed repository changes safely after code or documentation updates.
---

# git-auto-push Skill

Use this skill when a change is complete and should be committed and pushed to GitHub.

## Safety Rules

Never force push unless explicitly instructed.
Never push secrets.
Never stage the entire repository without reviewing changed files.
Never claim a push succeeded unless `git push` completed successfully.
Never push directly to `main` unless the user explicitly approved it.

## Procedure

1. Inspect repository state.

```bash
git status --short
git branch --show-current
git remote -v
```

Check for risky files.

Refuse or ask for confirmation if changes include:

.env
*.keystore
*.jks
secrets.*
local.properties
private keys
API keys
passwords
tokens

Run validation.

For docs-only changes:

```bash
git diff --check
```

For Android/code changes:

```bash
git diff --check
./gradlew assembleDebug
```

Stage intentional files only.

```bash
git add <file1> <file2> <file3>
```

Commit.

```bash
git commit -m "AREA-ID: concise change summary"
```

Push current branch.

```bash
git push origin HEAD
```

Report result.

Include:

branch name
commit hash
validation command
push result
changed files

## Branch Rule

If the current branch is main and the user did not explicitly approve pushing to main, create a branch:

```bash
git checkout -b auto/<short-change-name>
```

Then commit and push that branch.
