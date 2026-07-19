# Build Status

## Result in this delivery environment (0.2.0)

- Source tree created: **yes**
- Kotlin/Compose application implemented: **yes**
- CFS RFID codec implemented and VERIFIED: **yes** — see below
- Unit tests created: **yes**
- GitHub Actions Android build workflow created: **yes**
- Full Gradle/Android build executed: **yes, by CI** — not locally in this
  delivery environment (network limitation, see below); real results in
  the "Real Gradle/Android CI results" section
- APK produced locally: **no** — produced by CI, see below
- AAB produced locally: **no** — produced by CI, see below
- Physical NFC test: **no**
- K2 Pro/CFS test: **no**

## What WAS actually verified in this environment

Unlike the 0.1.0 baseline, the codec is not just written — it was compiled
and run as real JVM bytecode during development, independent of the
Android Gradle toolchain:

1. `nfc/CfsCodec.kt` was compiled standalone with the Kotlin compiler
   (`kotlinc`, JVM target, no Android dependencies) alongside a small test
   harness.
2. The resulting jar was executed with `java -jar`.
3. Output matched the published golden vector exactly:
   `UID 35B94A19 -> Key B 239E7FE23653`, and the full encrypt/decrypt/parse
   round trip reproduced `1A5241201B3D010010000000033000000100000000000000`
   (HyperPLA/01001/0330=1.0kg/serial 000001/black) and the exact published
   ciphertext blocks (`07881A46...`, `E07623E5...`, `FAC8F075...`).
4. The same logic (verified independently in Python against the upstream
   source) matched the Kotlin output byte-for-byte.

This means the cryptographic core of the app is real and tested, not a
placeholder — but it has NOT been exercised against Android's actual
`android.nfc.tech.MifareClassic` API, a physical tag, or a physical K2 Pro.
Those remain the genuine open items (see HOLD_REGISTER.md H-004, H-011,
and FUNCTIONAL_DESCRIPTION.md §20).

`nfc/DeviceCompatibility.kt` (H-000's device-list gate) was verified the
same way after its 2026-07-18 update: compiled standalone (with small
local stubs standing in for `android.os.Build`/`android.nfc.Tag`/
`android.nfc.tech.MifareClassic`, since this file's pure decision logic —
`assessDeviceModel` — doesn't need real Android behavior to test, only
the field values it's handed) alongside `domain/Models.kt` and
`data/MaterialCodes.kt`. All three files plus `nfc/CfsCodec.kt` and their
test suites (`CfsCodecTest`, `FilamentMathTest`, `DeviceCompatibilityTest`)
compiled and ran together: **18/18 tests passed.** `DeviceCompatibilityTest`
specifically guards the Pixel 8/8 Pro Android-15 version gate and the
bare-`"PIXEL"`-substring bug a naive matcher would have (silently marking
every future, unverified Pixel model SUPPORTED).

## Real Gradle/Android CI results (2026-07-18)

Since `.github/workflows/android.yml` triggers on every push, and GitHub's
own Actions runners are not behind this environment's network
restrictions, the actual `gradle testDebugUnitTest lintDebug assembleDebug
bundleRelease` build has genuinely run twice so far, both from this
delivery environment's pushes:

1. **Run 1** (commit `d82c8aa`, the initial real-source replacement):
   failed at `compileDebugKotlin` — `OpenFilamentApp.kt` used
   `rememberSaveable` without importing it (it lives in
   `androidx.compose.runtime.saveable`, not the `androidx.compose.runtime.*`
   wildcard already imported). A second reported error ("cannot infer
   type parameter" on the `AnimatedContent` call using `tab`) was a
   cascading effect of the same root cause, not a separate bug.
2. Fixed by adding the missing import (commit `7d94069`).
3. **Run 3**: `compileDebugKotlin` succeeded — but `testDebugUnitTest`
   failed 16/25 tests, every one of them in `DeviceCompatibilityTest` and
   `AppStateWriteGateTest`, all with the same `NullPointerException`.
   Root cause: Android's real "for unit tests" placeholder jar returns
   `null` for `Build.MODEL`/`Build.MANUFACTURER` (never happens on an
   actual device), and `DeviceCompatibility.assessDeviceModel`'s
   `manufacturer: String = Build.MANUFACTURER` default parameter had a
   non-null Kotlin type — Kotlin emits an
   `Intrinsics.checkNotNullParameter` for every non-null parameter
   regardless of whether the value came from an explicit argument or a
   default expression, so the null default threw immediately on any call
   that didn't pass `manufacturer` explicitly (which was every call in
   the app and every test). This is exactly the kind of bug the
   standalone JVM harness below could not catch, because its hand-written
   `Build` stub used placeholder non-null strings instead of reproducing
   Android's actual null-string behavior in the unit-test jar.
4. Fixed by dropping the never-actually-used `manufacturer` parameter and
   making `model` nullable with a safe `?: ""` fallback inside the
   function (commit `24a67bf`). Re-verified with a corrected standalone
   stub (`Build.MODEL`/`MANUFACTURER` now `null`, matching reality) —
   confirms the fix and that this stub inaccuracy would have caught the
   bug from the start had it been used originally.
5. **Run 5 (commit `24a67bf`): GREEN.** `testDebugUnitTest`,
   `lintDebug`, `assembleDebug`, and `bundleRelease` all passed for
   real, and a debug APK + release AAB were produced and uploaded as
   CI artifacts (`OpenFilamentCFS-builds`). This is the first genuinely
   successful end-to-end build of this project — real Android Gradle
   Plugin, real compileSdk 36 platform, real AndroidX/Compose
   dependencies, on GitHub's own infrastructure, unmodified by anything
   specific to this delivery environment.
6. **Run 12 (commit `11437ac`, the spool-persistence commit): FAILED.**
   All 5 new `SpoolPersistenceTest` cases threw `RuntimeException`.
   Root cause: `android.jar`'s `org.json.JSONObject`/`JSONArray` are
   non-functional stubs under plain JVM unit tests (real behavior only
   exists on-device or via Robolectric) — no prior test had actually
   constructed one at runtime, so this had never surfaced. This commit
   was fast-forward-merged to `main` before the failure was caught
   (`main` briefly sat on a red build).
7. Fixed by adding `testImplementation("org.json:json:20231013")` to
   `app/build.gradle.kts` (commit `e962cd2`) — a real, functional
   org.json jar on the unit-test classpath takes precedence over
   `android.jar`'s stub. **Run 15 (commit `e962cd2`): GREEN**, on the
   feature branch. `main` was then fast-forwarded from `11437ac` to
   `e962cd2` (bringing in both this fix and the earlier submodule/
   batch-supplier commit `e0611de`), restoring `main` to a build that
   is confirmed green rather than blindly re-merging. `main`'s own
   resulting CI run (run 16, same commit/tree as run 15) is expected to
   pass for the same reason — see the Actions tab to confirm.

`main` and the feature branch (`claude/openfilamentcfs-spoolmansync-setup-7pl0eb`)
are now in sync at `e962cd2`.

This is a materially stronger verification signal than the standalone
JVM harness below: it's the real Android Gradle Plugin, the real
compileSdk 36 platform, and the real AndroidX/Compose dependency set,
running unmodified. Treat CI's `compileDebugKotlin`/`assembleDebug`
result as authoritative over anything claimed by the standalone-compile
sections below, which only ever covered the non-UI logic files.

## Why the full Android build can't run inside this delivery environment

This environment has Java 21 and a standalone Kotlin compiler (fetched
from Maven Central, since no `kotlinc` CLI is preinstalled), but no
Android SDK. This isn't a missing-package problem: Android's SDK
platform/build-tools and the Android Gradle Plugin/AndroidX/Compose
artifacts are only ever served from `dl.google.com` (`maven.google.com`
is not an independent mirror — every real artifact path there 301s back
to `dl.google.com`), and this environment's network policy blocks that
host outright (confirmed directly: manifest requests to `ghcr.io` for a
Docker-based Android SDK image succeed, but the actual image layers,
hosted on `pkg-containers.githubusercontent.com`, are blocked the same
way). This is an environment limitation, not evidence that the source
fails to compile as an Android project — the CI workflow
(`.github/workflows/android.yml`) is the first authoritative full-project
compile check.

## Recommended first build

1. Install current Android Studio.
2. Open the project root.
3. Allow Android Studio to install Android API 36 and build-tools.
4. Generate the wrapper if required: `gradle wrapper --gradle-version 8.13`.
5. Run:

```bash
./gradlew clean testDebugUnitTest lintDebug assembleDebug bundleRelease
```

6. Confirm `CfsCodecTest` passes (it must — this is the gate for enabling
   the write path per FUNCTIONAL_DESCRIPTION.md §8.4). It exercises the same
   code already verified standalone in this environment, so it is expected
   to pass immediately.
7. Correct any dependency version drift reported by the current SDK/AGP
   tooling.
8. Install the debug APK on at least two physical NFC phones spanning
   different NFC controller vendors (H-000) and confirm the device-
   compatibility banner and tag-scan gate behave correctly before testing
   any real write.
9. Only once real MIFARE Classic 1K tags and a K2 Pro/CFS are available,
   proceed to H-004/H-011 hardware verification.

## Expected release files

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`

The release AAB will be unsigned or locally signed until the owner
configures an upload key and Play App Signing (not currently planned —
this is a personal/GitHub build first).
