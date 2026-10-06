#!/usr/bin/env bash
# Runs all AF9 linters: scripts (recipes, multiblocks, machines), quests, assets (textures, models, lang), facts (numbers in quests against recipes and Java), docs (paths, links, sections), links (KubeJS to Java), previews (multiblock preview pages without holes).
#
#   bash tools/lint/run.sh
#
# GT_SRC=<a checkout of GregTechCEu/GregTech-Modern> additionally checks the textures and lang keys of GT itself.
# Exit code 1 when any linter finds an error (warnings and notes do not fail).
cd "$(dirname "$0")/../.." || exit 2
status=0
echo "== scripts"; node tools/lint/scripts.js . || status=1
echo; echo "== quests"; python3 tools/lint/quests.py . || status=1
echo; echo "== assets"; python3 tools/lint/assets.py . || status=1
echo; echo "== facts"; python3 tools/lint/facts.py . || status=1
echo; echo "== docs"; python3 tools/lint/docs.py . || status=1
echo; echo "== links"; python3 tools/lint/links.py . || status=1
echo; echo "== previews"; python3 tools/lint/previews.py . || status=1
exit $status
