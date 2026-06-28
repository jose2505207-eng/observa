#!/usr/bin/env bash
# OBSERVA production validation. Honest gate: hard checks (build, tests, no-INTERNET) fail the
# script; v2.0.0-only checks (real model bundled, map pack) are reported as WARN until they land,
# so this is useful now and becomes the strict v2.0.0 gate once those exist.
set -uo pipefail
cd "$(dirname "$0")/.."

PASS=0; FAIL=0; WARN=0
ok()   { echo "  PASS  $1"; PASS=$((PASS+1)); }
bad()  { echo "  FAIL  $1"; FAIL=$((FAIL+1)); }
warn() { echo "  WARN  $1"; WARN=$((WARN+1)); }

echo "== OBSERVA production validation =="

echo "[1/6] assembleDebug"
if ./gradlew -q clean assembleDebug >/tmp/observa_build.log 2>&1; then ok "build"; else bad "build (see /tmp/observa_build.log)"; fi

echo "[2/6] unit tests"
if ./gradlew -q testDebugUnitTest >/tmp/observa_test.log 2>&1; then ok "unit tests"; else bad "unit tests (see /tmp/observa_test.log)"; fi

APK="app/build/outputs/apk/debug/app-debug.apk"
echo "[3/6] APK present + no INTERNET"
if [ -f "$APK" ]; then
  ok "APK present: $APK"
  AAPT=$(ls "$HOME/Android/Sdk/build-tools/"*/aapt2 2>/dev/null | head -1)
  if [ -n "$AAPT" ]; then
    if "$AAPT" dump permissions "$APK" 2>/dev/null | grep -qi "android.permission.INTERNET"; then
      bad "APK declares INTERNET permission"
    else
      ok "APK has no INTERNET permission"
    fi
  else
    warn "aapt2 not found; could not check permissions"
  fi
else
  bad "APK missing (build failed?)"
fi

echo "[4/6] real detector model (v2.0.0 requirement)"
if [ -f app/src/main/assets/models/observa_detector.pte ]; then
  ok "observa_detector.pte bundled"
  for f in observa_detector.labels observa_detector.metadata.json observa_detector.sha256; do
    [ -f "app/src/main/assets/models/$f" ] && ok "$f" || warn "$f missing"
  done
else
  warn "observa_detector.pte NOT bundled — detector runs heuristic fallback (see docs/REAL_MODEL.md / scripts/export_detector.py)"
fi

echo "[5/6] offline map pack (v2.0.0 requirement)"
if ls app/src/main/assets/maps/*.observamap.json app/src/main/assets/maps/*/manifest.json >/dev/null 2>&1; then
  ok "map pack present"
else
  warn "no bundled map pack — navigation uses demo location (see docs/MAP_PACKS.md)"
fi

echo "[6/6] key docs"
for d in README.md docs/FINAL_DEMO.md docs/KNOWN_LIMITATIONS.md docs/PRIVACY_MODEL.md \
         docs/PERFORMANCE_METRICS.md docs/ACCESSIBILITY_VALIDATION.md docs/HAPTIC_LANGUAGE.md \
         docs/RELEASE_NOTES.md; do
  [ -f "$d" ] && ok "$d" || warn "$d missing"
done

echo "== Summary: PASS=$PASS WARN=$WARN FAIL=$FAIL =="
if [ "$FAIL" -gt 0 ]; then
  echo "RESULT: FAIL (hard checks failed)"; exit 1
fi
if [ "$WARN" -gt 0 ]; then
  echo "RESULT: PASS (shippable) but NOT v2.0.0-ready — $WARN warning(s) block the v2.0.0 gate."; exit 0
fi
echo "RESULT: PASS — v2.0.0 production checks satisfied."; exit 0
