# Build Status

## Result in this delivery environment (0.2.0)

- Source tree created: **yes**
- Kotlin/Compose application implemented: **yes**
- CFS RFID codec implemented and VERIFIED: **yes** — see below
- Unit tests created: **yes**
- GitHub Actions Android build workflow created: **yes**
- Full Gradle/Android build executed: **no** (environment limitation, see below)
- APK produced locally: **no**
- AAB produced locally: **no**
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

## Why the full Android build wasn't run

This environment has Java 21 and (as of this revision) a standalone Kotlin
compiler, but no Android SDK, no Gradle installation, and no Gradle wrapper
JAR, and no way to install the Android SDK/build-tools/emulator stack
within this session. This is an environment limitation, not evidence that
the source fails to compile as an Android project — the CI workflow
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
