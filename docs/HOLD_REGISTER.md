# Hold and Dependency Register

Version: 0.2.0 — revised alongside FUNCTIONAL_DESCRIPTION.md 0.2.0.
Changes from the prior register: H-001 is downgraded from a research/design
blocker to a bench-verification task; a new H-000 is added as the true
top-priority item; several other items are downgraded from "design unsolved"
to "confirm against real hardware," reflecting that the design questions
themselves are answered by existing open-source work. See
docs/FUNCTIONAL_DESCRIPTION.md and the research report referenced there for
full reasoning and citations.

## H-000 — Android device MIFARE Classic support (NEW — top priority)

**Status:** HOLD — release blocker for real tag writing, promoted ahead of
the former H-001.
**Reason:** `android.nfc.tech.MifareClassic` is an optional Android API,
implemented only on NXP-compatible NFC controllers. Devices with Broadcom or
certain other controllers cannot enumerate `MifareClassic` at all and can
only see the tag's UID. Some newer flagship devices have also been reported
to fail MIFARE Classic operations with non-default keys despite NXP-class
hardware. iOS is out of scope entirely (Apple restricts this at the OS
level).
**Needed to close:**

- ~~a maintained allow/deny device list, seeded from community-maintained
  MIFARE Classic Tool compatibility data~~ — **done 2026-07-18:**
  `nfc/DeviceCompatibility.kt` now sources its `KNOWN_COMPATIBLE`/
  `KNOWN_INCOMPATIBLE` sets directly from ikarus23/MifareClassicTool's
  `COMPATIBLE_DEVICES.md`/`INCOMPATIBLE_DEVICES.md`, restricted to entries
  where `Build.MODEL` reliably contains the matched string (Pixel devices,
  plus the CFSWriter-confirmed Galaxy S25/S26 model codes) — most of the
  community lists use marketing names that don't appear in `Build.MODEL`
  on non-Pixel hardware, so those are kept verbatim in
  `COMMUNITY_REPORTED_*_MARKETING_NAMES` for manual reference rather than
  wired into the automated check, to avoid a false SUPPORTED verdict. This
  pass also fixed a real error: Pixel 8/8 Pro was hard-coded UNSUPPORTED
  from an anecdotal XDA report, but the authoritative list shows Google
  fixed it in Android 15+ — the code now gates on `Build.VERSION.SDK_INT`
  instead of blanket-refusing those two models. Covered by
  `DeviceCompatibilityTest` (9 tests, standalone-verified per
  BUILD_STATUS.md);
- a `getTechList()`-based capability check that runs before any write
  attempt, not mid-transaction — **already implemented** (`assessTag`);
- device-code translation for the remaining marketing-name-only community
  entries (Samsung/Xiaomi/etc. report model codes, not marketing names, in
  `Build.MODEL`) — open;
- bench confirmation on at least two real device models spanning different
  NFC controller vendors — open, needs physical hardware.

**User impact:** on unsupported hardware, the app must refuse the write flow
immediately with a specific, honest message, rather than attempting and
failing a write.

## H-001 — CFS codec and authentication (DOWNGRADED — was the prior top blocker)

**Status:** substantially resolved by existing open-source work; remaining
work is bench verification, not design or key-recovery.
**What changed:** the CFS tag's MIFARE sector Key B is not a secret to be
supplied or attacked — it is deterministically derived from the tag's own
UID via AES under a firmware master key, and this is documented with a
reproducible golden vector (UID `35B94A19` → Key B `239E7FE23653` → known
plaintext) in existing open-source work. A working Kotlin/Compose Android
implementation of the full codec already exists and is licensed for forking.
**Implemented:** MIFARE Classic 1K probing, authenticated read interface,
block-size validation, Blocks 4–6 write transport, immediate read-back
verification (unchanged from prior revision).
**Needed to close:**

- fork the existing Kotlin codec (or port the existing Python reference)
  rather than reimplement;
- pass the golden-vector regression test (FUNCTIONAL_DESCRIPTION.md §8.4);
- resolve the ECB-vs-CBC block-cipher-mode question empirically against the
  golden vector (§8.2) — this is a same-day bench task, not open research;
- confirm the sector-trailer write behaviour differs correctly between
  blank tags and re-used factory tags (§8.6/§20);
- confirm write reliability on real hardware post H-000.

**User impact:** the write button can now be enabled once the above bench
steps pass, rather than remaining permanently disabled pending unknown key
recovery.

## H-002 — Gradle/Android SDK build verification

**Status:** CLOSED as of 2026-07-18. `.github/workflows/android.yml` runs
on every push, on GitHub's own Actions runners (not subject to this
delivery environment's network restrictions), and has now genuinely
passed: `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and
`bundleRelease` all green, with a debug APK and release AAB produced and
uploaded as CI artifacts. See BUILD_STATUS.md for the full run history,
including three real bugs this exposed and fixed along the way (a
missing `rememberSaveable` import, a `NullPointerException` from
`Build.MODEL`/`MANUFACTURER` being `null` in the real unit-test jar, and
two deprecated icon references).
**Needed to close:** nothing further for the build itself — this is
closed. Installing the resulting APK on physical hardware remains H-000/
H-004/H-011's job, not this item's.

## H-003 — Play Store signing and upload

**Status:** unchanged — owner-controlled credentials, deprioritised further
since this build is for personal/GitHub use first. Only needed if/when
public distribution is decided.

## H-004 — K2 Pro capability adapter (DOWNGRADED — schema now documented, target model unverified)

**Status:** HOLD — verification against the specific target model, not
against an unknown protocol.
**What changed:** the K2-series `box` Moonraker object schema (per-slot
`color_value`, `material_type`, `remain_len`, `temperature`,
`dry_and_humidity`, `state`, plus `auto_refill`/`same_material`/`map`), the
`M8200` load/unload command family, the `M141` chamber command, and the
WebRTC:8000 camera endpoint are all documented by existing community
reverse-engineering work.
**Caveat:** that documentation is confirmed against the **K2 Plus**, not the
K2 Pro that this project targets; K2/K2 Pro are explicitly marked untested
in the same source.
**Needed to close:** probe a real K2 Pro and confirm the same object names,
command set, and endpoints apply; note and adapt to any Pro-specific
differences.

## H-005 — Automatic spool joining (DOWNGRADED TWICE — native feature confirmed; slot-grouping display is now SpoolmanSync's job, not the app's)

**Status:** design resolved; verification against hardware remains, and only
for a one-line toggle, not a display.
**What changed:** automatic relay-on-runout is a native CFS firmware
feature (`BOX_ENABLE_AUTO_REFILL`, reflected in `box.auto_refill` /
`box.same_material`), doing same-type/same-colour matching internally. The
previously-planned Strict/Compatible/Manual-approved matching-tier design in
the app is unnecessary and is retired. As of this revision, the app's scope
here is further cut to a single toggle switch — visibility into *which*
slots are grouped as mutual backups is now recommended to come from
**SpoolmanSync** (FUNCTIONAL_DESCRIPTION.md §11.1), not an app-built slot
dashboard.
**Needed to close:** confirm the toggle command (`BOX_ENABLE_AUTO_REFILL`)
behaves as documented on a real K2 Pro/CFS. Slot-grouping *display*
correctness is SpoolmanSync's acceptance criterion, not this app's.

## H-006 — Active chamber control

**Status:** unchanged in substance — still HOLD for active control, still
advisory-only in v1. The printer-side primitives to build on if this is ever
pursued (M141/M191 macros, Klipper's `verify_heater` watchdog) are now
identified, but no active-control work is planned.
**Needed to close:** unchanged from prior revision if ever pursued.

## H-007 — Spoolman automatic deduction (DOWNGRADED TWICE — this is now entirely SpoolmanSync's job)

**Status:** resolved as a hand-off, not an app feature. No app-side
consumption UI, ledger, or display is planned at all.
**What changed:** Moonraker's own `[spoolman]` component already performs
job-level, idempotent consumption tracking. More importantly, **SpoolmanSync**
(gibz104/SpoolmanSync — MIT, Docker-deployable, explicit K1/K2/K2 Plus/Hi/
Ender-3-V3-CFS support via `ha_creality_ws`) already ships a complete,
working dashboard for exactly this: automatic per-print weight deduction,
low-stock alerts, and "RFID auto-match" that recognises a tagged CFS spool
and keeps it synced with Spoolman. The app's job ends at writing the
serial-number-as-spool-ID convention into the tag at tagging time (§9,
§14) — everything downstream is SpoolmanSync's, deployed separately.
**Needed to close (app side):** nothing further — write the serial
convention into the tag correctly (already implemented) and stop there.
**Needed to close (deployment side, not app code):** deploy SpoolmanSync +
`ha_creality_ws` alongside Moonraker; confirm the RFID auto-match
recognises tags written by this app on a real K2 Pro.

## H-008 — Camera feed (DOWNGRADED — endpoint documented)

**Status:** verification remaining, not discovery.
**What changed:** the K2 series serves camera over WebRTC on port 8000; an
existing Home Assistant integration already wraps this exact path (listing
K2 Pro as supported) and is a usable reference for the embedding approach.
**Needed to close:** confirm the endpoint and auth scheme on a real K2 Pro;
decide whether to embed WebRTC directly or reuse a go2rtc-style relay
component.

## H-009 — Closed-loop servo diagnostics

**Status:** unchanged — HOLD, and expected to remain permanently absent
rather than "pending validation."
**What changed:** confirmed that K2 servo faults surface as printer/UI error
codes (e.g. peak-current-protection, extruder-error codes), not as
documented Moonraker following-error/motor-current/driver-fault objects.
The capability-probe design is retained as a mechanism, but the default
expectation is now explicitly "not available," not "not yet validated."
**Needed to close:** re-run the capability probe if Creality or a community
Klipper fork ever exposes genuine per-axis telemetry; otherwise no further
action.

## H-010 — Full production catalogue

**Status:** unchanged — SpoolmanDB ingestion for filament facts, plus a
separately-governed Creality material-code mapping layer (~56 known codes),
schema pipeline, caching, atomic update, rollback and provenance review
remain open.

## New: H-011 — Firmware-update tag compatibility drift

**Status:** HOLD — ongoing risk, not a one-time close item.
**Reason:** community reports describe Creality firmware updates breaking
previously-working custom-written tags.
**Needed to close:** no permanent close is possible; the app should surface
a clear "verified against firmware version X" indicator per mapping, and a
process for re-validating after printer firmware updates.
