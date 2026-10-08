#!/usr/bin/env bash
# Builds the AF9 fork of GTCEu and publishes it to mavenLocal: upstream's release (upstream.properties) with the patches of
# patches/ applied. Usage: gtceu-fork/build.sh <version>   (af9-core's gtceu_fork_version, e.g. 7.2.0-af9.1)
# af9-core's build runs this itself where the fork is not in mavenLocal yet; run it by hand to rebuild after a patch change.
set -euo pipefail
version="${1:?usage: build.sh <fork version>}"
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(grep '^repo=' "$here/upstream.properties" | cut -d= -f2-)"
tag="$(grep '^tag=' "$here/upstream.properties" | cut -d= -f2-)"
work="${AF9_FORK_WORK:-$here/.build}"

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
