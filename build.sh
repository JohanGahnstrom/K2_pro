#!/usr/bin/env bash
set -euo pipefail
IMAGE="${ANDROID_IMAGE:-ghcr.io/cirruslabs/android-sdk:latest}"
docker pull "$IMAGE"
docker run --rm --pull=always -v "$PWD:/workspace" -v openfilament-gradle:/root/.gradle -w /workspace "$IMAGE" bash -lc './gradlew test lint assembleDebug bundleRelease'
