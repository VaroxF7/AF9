#!/usr/bin/env bash
# Writes the commits you made in gtceu-fork/.build (the fork's checkout, left there by build.sh) to patches/, replacing the
# old ones. Then raise gtceu_fork_version in af9-core/gradle.properties (or just rebuild: the build also notices changed patches).
set -euo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
tag="$(grep '^tag=' "$here/upstream.properties" | cut -d= -f2-)"
work="${AF9_FORK_WORK:-$here/.build}"
[ -d "$work/.git" ] || { echo "no checkout at $work: build the fork once first" >&2; exit 1; }
[ -z "$(git -C "$work" status --porcelain)" ] || { echo "commit your changes in $work first" >&2; exit 1; }
rm -f "$here"/patches/*.patch
git -C "$work" format-patch --quiet "$tag"..HEAD -o "$here/patches"
echo "$(ls "$here"/patches/*.patch 2>/dev/null | wc -l) patch(es) written to gtceu-fork/patches/"
