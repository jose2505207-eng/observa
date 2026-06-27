# Development Loops

Repeatable loops so the team stops guessing. Pick a loop that matches your task. All loops end by validating ([[Validation Gates]]) and, if behavior changed, updating the wiki.

## Loop 1: Feature Loop
1. Pick a requirement ID from [[Product Requirements]].
2. Create branch: `feature/REQ-ID-short-name`.
3. Implement the smallest working slice.
4. Run local validation (relevant gate).
5. Update wiki if behavior changes.
6. Commit with the requirement ID.
7. Push branch.
8. Open a PR or notify the team.
9. Demo locally.
10. Merge only after the validation gate passes.

## Loop 2: AI Runtime Loop
1. Confirm target model.
2. Confirm input shape.
3. Confirm preprocessing.
4. Run dummy inference.
5. Run real-frame inference.
6. Measure latency.
7. Confirm backend (truthfully — see [[Technical Architecture]] Truthful Backend Policy).
8. Add fallback (CPU).
9. Document truthfully.
10. Add dashboard status (REQ-007).

## Loop 3: Accessibility Loop
1. Use the phone without looking at the screen.
2. Enable TalkBack.
3. Test launch.
4. Test the core action.
5. Test alert clarity.
6. Test no repeated spam.
7. Test haptics.
8. Fix confusing wording.
9. Document the result.

## Loop 4: Demo Loop
1. Reset app state.
2. Enable Airplane Mode.
3. Start camera.
4. Show dashboard.
5. Trigger a hazard.
6. Trigger text/OCR if available.
7. Trigger voice or manual prompt.
8. Show the privacy proof.
9. Tell the judge what is local.
10. Record a backup video.

## Loop 5: Performance Loop
1. Start camera.
2. Record FPS.
3. Record inference latency.
4. Record dropped frames.
5. Check memory.
6. Check heat/battery notes.
7. Optimize the bottleneck.
8. Update the benchmark table.

## Loop 6: Documentation Loop
1. Change behavior.
2. Update code comments only where useful.
3. Update README if setup changed.
4. Update the wiki page if workflow changed.
5. Add a [[Troubleshooting]] note if a bug was confusing.
6. Commit docs with the code or immediately after.
