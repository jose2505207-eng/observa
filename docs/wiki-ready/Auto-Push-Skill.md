# Auto Push Skill

The `git-auto-push` skill validates, commits, and pushes completed changes safely — preventing broken builds, secret leaks, and accidental destructive or direct-to-`main` pushes.

The actual skill file lives in the repository at **`.claude/skills/git-auto-push/SKILL.md`** and is reproduced below.

## Trigger
Use after a completed code or documentation change when:
- the user asks to save/push changes, **or**
- the change is complete and validation passes, **or**
- `AUTO_PUSH=true` is set or the user explicitly requested automatic pushing.

## Refusal cases
Refuse (or ask first) when:
- the build fails,
- a secret is detected,
- `git status` shows unknown/suspicious files,
- the current branch is `main` and pushing `main` was not approved,
- a meaningful commit message cannot be formed,
- a merge conflict exists,
- the remote is missing.

## Behavior summary
1. Detect changed files (`git status --short`).
2. Refuse if dangerous files are staged/modified: `.env`, `*.keystore`, `*.jks`, `secrets.*`, `local.properties`, or files containing API keys/tokens/passwords/private keys.
3. Lightweight checks: Android changes → `./gradlew assembleDebug`; docs-only → skip Gradle; wiki-only → verify markdown exists and links are reasonable; always `git diff --check`.
4. Stage only intentional files (`git add <files>`; never `git add .` unreviewed).
5. Commit with a structured message `AREA-ID: description`.
6. Push current branch (`git push origin HEAD`).
7. Report branch, commit hash, files changed, validation result, and remote/branch pushed.
8. Never force push unless explicitly instructed.
9. Never push directly to `main` unless explicitly allowed.
10. If on `main` without approval, create `auto/<short-change-name>` first.

---

## Skill file content (`.claude/skills/git-auto-push/SKILL.md`)

````markdown
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
````

See [[GitHub Workflow]] for branch and commit conventions and [[Validation Gates]] Gate I.
