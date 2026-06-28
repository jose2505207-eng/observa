# Release checklist

Run before tagging any `vX.Y.0`. Keep `main` always shippable.

## Gates (must pass)
- [ ] `./gradlew clean assembleDebug` succeeds.
- [ ] `./gradlew testDebugUnitTest` succeeds (record count; currently 106).
- [ ] APK has **no `INTERNET`** permission: `aapt2 dump permissions app-debug.apk | grep -i internet` empty.
- [ ] App launches in **Airplane mode**; camera preview visible; no crash.
- [ ] OCR, voice, Braille/TalkBack status, audio cues, haptics, foreground service, navigation all still function (no regressions).
- [ ] Heuristic fallback remains honest if no model is bundled (`AI model: unavailable — heuristic fallback`).
- [ ] No fabricated claims in UI/docs: no fake object labels, no "QNN active" without `getBackends()` proof, no "physical Braille verified", no GPS precision claims.

## Docs
- [ ] `README.md` "Current status" matches reality.
- [ ] `wiki/log.md` has a dated entry; affected wiki pages updated (single source of truth).
- [ ] `docs/RELEASE_NOTES.md` updated.
- [ ] Relevant `docs/manual-test-*.md` and `docs/device-validation/*` updated.

## Git (per-version merge protocol)
- [ ] `git fetch origin && git checkout main && git pull --ff-only origin main`
- [ ] `git checkout feature/<branch> && git rebase main`
- [ ] build + test + APK permission check
- [ ] `git checkout main && git merge --ff-only feature/<branch>`
- [ ] `git push origin main`
- [ ] `git tag -a <tag> -m "…" && git push origin <tag>`

## Final validation matrix (fill per release)
| Area | Result | Evidence |
|---|---|---|
| camera | | |
| detector (heuristic / model) | | |
| OCR | | |
| audio cues | | |
| haptics | | |
| voice | | |
| TalkBack | | |
| Braille (app-level / physical) | | |
| foreground service | | |
| navigation | | |
| airplane mode / no INTERNET | | |
| QNN (active?) | | |
| latency | | |
| battery/thermal | | |
