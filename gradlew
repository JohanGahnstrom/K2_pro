#!/usr/bin/env bash
set -euo pipefail
V=9.3.1
D="$HOME/.gradle/wrapper/dists/gradle-$V-bin/local/gradle-$V"
if [ ! -x "$D/bin/gradle" ]; then mkdir -p "$(dirname "$D")"; curl -fL "https://services.gradle.org/distributions/gradle-$V-bin.zip" -o /tmp/gradle.zip; unzip -q /tmp/gradle.zip -d "$(dirname "$D")"; fi
exec "$D/bin/gradle" "$@"
