#!/usr/bin/env bash
# Builds the AF9 fork of GTCEu and publishes it to mavenLocal: upstream's release (upstream.properties) with the patches of
# patches/ applied. Usage: gtceu-fork/build.sh <version>   (af9-core's gtceu_fork_version, e.g. 7.5.3-af9.1)
# af9-core's build runs this itself where the fork is not in mavenLocal yet; run it by hand to rebuild after a patch change.
set -euo pipefail
version="${1:?usage: build.sh <fork version>}"
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(grep '^repo=' "$here/upstream.properties" | cut -d= -f2-)"
tag="$(grep '^tag=' "$here/upstream.properties" | cut -d= -f2-)"
work="${AF9_FORK_WORK:-$here/.build}"

# a checkout with work in it (changes or commits not yet exported to patches/) is never thrown away
if [ -d "$work/.git" ]; then
    applied=$(ls "$here"/patches/*.patch 2>/dev/null | wc -l)
    ahead=$(git -C "$work" rev-list --count "$tag"..HEAD 2>/dev/null || echo 0)
    if [ -n "$(git -C "$work" status --porcelain)" ] || [ "$ahead" -gt "$applied" ]; then
        echo "gtceu-fork/.build has changes that are not in patches/: run gtceu-fork/export-patches.sh first" >&2
        exit 1
    fi
fi
rm -rf "$work"
git clone --quiet --depth 1 --branch "$tag" "$repo" "$work"
cd "$work"
git config user.name "AF9"
git config user.email "af9@localhost"
shopt -s nullglob
for patch in "$here"/patches/*.patch; do
    echo "applying $(basename "$patch")"
    git am --quiet --3way "$patch"
done
chmod +x gradlew
./gradlew publishToMavenLocal -Pmod_version="$version" -x test --stacktrace
echo "GTCEu fork $version published to mavenLocal"
