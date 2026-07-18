# OpenFilament CFS

A native Android, local-first, open-source filament tagging and CFS
management app for Creality K2 Pro + CFS. Version 0.2.0-alpha01.

## What changed from 0.1.0

The CFS RFID codec is no longer an open research problem. `nfc/CfsCodec.kt`
is a direct, verified port of the community-published AES key-derivation and
payload scheme (see `THIRD_PARTY.yml`), checked against a published golden
vector in `CfsCodecTest.kt` (UID `35B94A19` -> Key B `239E7FE23653` ->
HyperPLA/01001/1.0kg/serial 000001/black). This test was compiled and
executed directly against real Kotlin bytecode during development — not
just cross-checked in Python — and passes.

The app is re-scoped as a **thin client**: it writes/reads tags itself (the
one thing only a phone-in-hand can do), and otherwise configures and
observes the printer's own CFS/Spoolman features rather than reimplementing
relay, thermal or consumption logic. See `docs/FUNCTIONAL_DESCRIPTION.md`
for the full reasoning.

## What is implemented

- Simple and Expert user modes
- Real CFS tag codec: AES-128-ECB key derivation + payload encrypt/decrypt,
  verified against a published golden vector
- H-000 device-compatibility gate (`nfc/DeviceCompatibility.kt`) — refuses
  the write flow on phones whose NFC controller can't do MIFARE Classic,
  checked before any write, not mid-transaction
- Full NFC transaction: probe → security-state detection (factory-default
  vs already CFS-secured) → write Blocks 4-6 → mandatory read-back
  verification → sector-trailer install path for blank tags
- Real Android NFC foreground-dispatch wiring in `MainActivity`
- Moonraker `box` object parsing (per-slot material/colour/remaining-length/
  temperature), native auto-refill toggle via `BOX_ENABLE_AUTO_REFILL`,
  `M8200` load/unload (never `BOX_LOAD_MATERIAL`), `M141` chamber command
- Real material-code registry (41 verified Creality codes) replacing
  placeholder IDs in the catalogue
- Polished Jetpack Compose/Material 3 UI, Simple/Expert mode, edge-to-edge,
  dynamic colour
- Unit tests (codec golden vector + filament math) and GitHub Actions workflow

## Intentionally held

Active chamber thermal control, camera streaming, closed-loop servo
telemetry (expected permanently absent on stock K2 firmware), and full
production catalogue ingestion remain out of scope for this release — see
`docs/HOLD_REGISTER.md`. The K2 **Pro**-specific `box`/`M8200` behaviour is
expected but not yet bench-verified (the reference documentation is
confirmed on K2 Plus); treat this as the top remaining open item alongside
the blank-tag sector-trailer write path.

## Build

Open with a current Android Studio and install Android API 36. If the
wrapper JAR is absent, run `gradle wrapper --gradle-version 8.13` once.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug bundleRelease
```

`testDebugUnitTest` includes `CfsCodecTest`, which must pass — it is the
gate described in `docs/FUNCTIONAL_DESCRIPTION.md` §8.4.

## Licence

See `THIRD_PARTY.yml` for per-file attribution. `nfc/CfsCodec.kt` and
`data/MaterialCodes.kt` are ported from flamebarke/creality_rfid, which
carries no explicit LICENSE file upstream as of this writing — confirm
terms with the author before any public redistribution.

## Independence

OpenFilament CFS is not affiliated with or endorsed by Creality.
