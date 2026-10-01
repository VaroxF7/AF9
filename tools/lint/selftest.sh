#!/usr/bin/env bash
# Self-test of the linters: copies the repository to a scratch directory, puts known mistakes in, and checks that every one is found.
# If a rule stops finding its mistake, this fails: a linter that finds nothing proves nothing.
#
#   bash tools/lint/selftest.sh        (from the repository root)
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/tools" "$TMP/af9-core/src/main" "$TMP/config"
cp -r "$ROOT/kubejs" "$TMP/kubejs"
cp -r "$ROOT/config/ftbquests" "$TMP/config/ftbquests"
cp -r "$ROOT/af9-core/src/main/java" "$ROOT/af9-core/src/main/resources" "$TMP/af9-core/src/main/"
cp -r "$HERE" "$TMP/tools/lint"
rm -rf "$TMP/tools/lint/selftest"

# --- scripts: fixtures with mistakes ------------------------------------------------------------------------------------
cp "$HERE/selftest/startup.js" "$TMP/kubejs/startup_scripts/zz_selftest.js"
cp "$HERE/selftest/server.js" "$TMP/kubejs/server_scripts/zz_selftest.js"

# --- quests: a duplicate id, a dead dependency, a missing text, a missing item, a bad id ----------------------------------------
python3 - "$TMP" <<'PY'
import re, sys, pathlib
root = pathlib.Path(sys.argv[1])
p = root / 'config/ftbquests/quests/chapters/photolithography.snbt'
t = p.read_text(encoding='utf-8')
ids = re.findall(r'\n\t\t\tid: "([0-9A-F]{16})"', t)
# Q1: the id of the second quest copied to the first
t = t.replace(f'id: "{ids[0]}"', f'id: "{ids[1]}"', 1)
# Q3: a dependency on a quest that does not exist
t = t.replace('dependencies: [', 'dependencies: [\n\t\t\t\t"DEADBEEFDEADBEEF"', 1)
# Q5: a text that is in no lang file
t = t.replace('"{af9.quest.litho.busConnector.1}"', '"{af9.quest.litho.busConnector.99}"', 1)
# Q6: an item nobody defines
t = t.replace('kubejs:calibration_wafer', 'kubejs:selftest_no_such_item', 1)
# Q2: an id that is not hex
t = t.replace(f'id: "{ids[5]}"', 'id: "NOTHEXNOTHEXNOTH"', 1)
p.write_text(t, encoding='utf-8')

# --- assets: a texture that is not square, a broken .mcmeta, a duplicate lang key, a bad format code, a model without texture
tex = root / 'kubejs/assets/kubejs/textures/item'
(tex / 'selftest_wide.png').write_bytes(open(next(tex.rglob('*.png')), 'rb').read())
import struct
b = bytearray((tex / 'selftest_wide.png').read_bytes()); b[16:24] = struct.pack('>II', 32, 16); (tex / 'selftest_wide.png').write_bytes(bytes(b))
(tex / 'selftest_wide.png.mcmeta').write_text('{ not json', encoding='utf-8')
lang = root / 'kubejs/assets/kubejs/lang/en_us.json'
t = lang.read_text(encoding='utf-8')
t = t.rstrip().rstrip('}').rstrip() + ',\n\t"af9.selftest.dup": "one",\n\t"af9.selftest.dup": "two",\n\t"af9.selftest.fmt": "50%d done"\n}\n'
t = t.replace('Fluids per print: 300 TMAH', 'Fluids per print: 301 TMAH', 1)   # X1: a quest text that no longer matches its recipe
lang.write_text(t, encoding='utf-8')
m = root / 'af9-core/src/main/resources/assets/af9/models/item/nano_cpu_card.json'
m.write_text(m.read_text(encoding='utf-8').replace('af9:item/nano_cpu_card', 'af9:item/selftest_no_texture'), encoding='utf-8')
PY

fail=0
check() {   # check <name> <output> <code...>
  local name="$1" out="$2"; shift 2
  for code in "$@"; do
    if ! grep -Eq "^(ERROR|WARN|INFO) +$code " <<<"$out"; then echo "SELFTEST FAIL: $name no longer finds $code"; fail=1; fi
  done
}
S="$(node "$TMP/tools/lint/scripts.js" "$TMP" 2>&1)"
check scripts "$S" S1 R1 R2 R3 R4 R5 R6 R7 R8 R9 R10 R11 R12 M1 M3 M4 L1
for typo in cleanroom_glas strange_matte_dust; do
  grep -q "gtceu:$typo" <<<"$S" || { echo "SELFTEST FAIL: scripts no longer find the GT typo gtceu:$typo"; fail=1; }
done
Q="$(python3 "$TMP/tools/lint/quests.py" "$TMP" 2>&1)"
check quests "$Q" Q1 Q2 Q3 Q5 Q6
A="$(python3 "$TMP/tools/lint/assets.py" "$TMP" 2>&1)"
check assets "$A" A1 A2 A4 A5 A6
X="$(python3 "$TMP/tools/lint/facts.py" "$TMP" 2>&1)"
check facts "$X" X1
if [ "${SELFTEST_VERBOSE:-}" = 1 ]; then echo "$S" | grep -E "selftest|^(ERROR|WARN)" | head -50; echo "$Q" | head -20; echo "$A" | head -20; fi
if [ "$fail" = 0 ]; then echo "selftest ok: every planted mistake was found"; else
  echo "--- scripts"; echo "$S" | head -60; echo "--- quests"; echo "$Q" | head -30; echo "--- assets"; echo "$A" | head -30; fi
exit $fail
