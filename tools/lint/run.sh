#!/usr/bin/env bash
# Runs all AF9 linters: scripts (recipes, multiblocks, machines), quests, assets (textures, models, lang).
#
#   bash tools/lint/run.sh [--selftest]
#
# GT_SRC=<a checkout of GregTechCEu/GregTech-Modern> additionally checks the textures and lang keys of GT itself.
# Exit code 1 when any linter finds an error (warnings and notes do not fail).
cd "$(dirname "$0")/../.." || exit 2
status=0
echo "== scripts"; node tools/lint/scripts.js . || status=1
echo; echo "== quests"; python3 tools/lint/quests.py . || status=1
echo; echo "== assets"; python3 tools/lint/assets.py . || status=1
if [ "${1:-}" = "--selftest" ]; then echo; echo "== selftest"; bash tools/lint/selftest.sh || status=1; fi
exit $status
