# OpenFilament CFS — Complete Functional Description

Version: 0.2.1-alpha01
Status: revised post-research baseline. Supersedes 0.1.0-alpha01 in full.
Target hardware: Creality K2 Pro + CFS (Cargo Feeding System)

Changes from 0.1.0: the RFID codec is no longer treated as an open cryptographic
problem — it is solved and forkable (§8). The single largest remaining risk is
moved from "we lack keys" to "we don't know which phones can even do this"
(§8.5, H-000). The application is re-scoped from a product that reimplements
printer automation to a **thin client**: an NFC tag writer plus a read-mostly
Moonraker/Home Assistant UI, with every stateful or time-critical function
delegated to the printer, its native firmware features, or an existing
persistent open-source service. See §19 for the full reuse map.

Changes from 0.2.0 (this revision): the app's own printer-facing ambition is
cut further, to near zero, following discovery of **SpoolmanSync**
(gibz104/SpoolmanSync) — a ready, MIT-licensed, Docker-deployable dashboard
that already does CFS slot display, consumption tracking and low-stock
alerts against real K1/K2/K2 Plus/Hi/Ender-3-V3-CFS hardware via
`ha_creality_ws`+Spoolman. See §11.1. The app's job narrows to: write the
tag, toggle native auto-refill, show basic online/temperature status, and
point the user at SpoolmanSync for everything else.

## 1. Purpose

OpenFilament CFS is a native Android application for identifying, cataloguing
and preparing third-party filament spools for use in a Creality K2 Pro fitted
with CFS. Its primary interaction is deliberately simple: select the filament,
confirm spool size and colour, then write and verify a compatible RFID tag.
The application is local-first, open source, independent of Creality, and
built by forking and wrapping existing open-source work wherever one exists,
rather than reimplementing it.

The product is split into two experiences:

- **Simple Mode** is the default. It hides protocol data, material IDs,
  encryption, block layouts, printer macros and raw telemetry. The user sees
  only understandable choices.
- **Expert Mode** exposes mapping evidence, technical values and
  capability-gated printer tools behind explicit warnings.

The application shall never present an unverified write or printer command as
safe merely because a community implementation exists — but as of this
revision, the CFS tag codec itself **is** a verified, reproducible community
result (§8), not an open question.

## 2. Product goals

1. Reduce third-party spool configuration to a few guided choices.
2. Preserve an expert route for troubleshooting and contribution.
3. Build on standard Android NFC rather than a proprietary accessory —
   subject to the hardware-support gate in §8.5.
4. Keep filament identity, protocol encoding and printer automation decoupled.
5. Work offline after catalogue installation, for the tagging workflow only.
6. Track data provenance and mapping confidence.
7. Verify every tag write by reading the blocks back.
8. **Never run time-critical or stateful printer logic inside the Android
   app.** Relay-on-runout, filament consumption accounting, and thermal
   control all execute printer-side or in an existing persistent service; the
   phone only configures and observes them (§12–§14).
9. Meet Google Play target API, privacy and packaging requirements.
10. Remain useful even when no printer or Spoolman server is connected.
11. **Minimise net-new code.** Before writing any subsystem, check §19's reuse
    map for an existing fork/dependency target. Net-new app code is reserved
    for the Simple/Expert UX layer and glue, not for protocol or automation
    logic that already exists in the open-source ecosystem.

## 3. User modes

### 3.1 Simple Mode

Simple Mode provides:

- manufacturer and product search;
- recent and favourite filament selection;
- manufacturer colour selection;
- standard spool weights (see §7 — these are quantised categories, not exact
  masses);
- automatic mapping selection against the Creality material-code table
  (§7.1);
- ordinary-language compatibility status;
- one-tap preparation for tag writing, gated by the device-capability check
  in §8.5;
- matching second-tag workflow;
- basic read mode;
- local spool list;
- read-only printer/CFS-slot dashboard where supported (§11);
- basic low-spool and material/chamber-temperature warnings, sourced from the
  printer, not calculated from the tag (§7, §13).

Simple Mode does not expose:

- sector numbers or block addresses;
- keys, UID derivation or encryption;
- raw bytes;
- vendor or material database identifiers;
- arbitrary G-code;
- SSH;
- macro installation;
- automatic heater control;
- native CFS commands other than the documented, capability-checked ones in
  §11–§12;
- unsupported servo/motion interpretations (§15 — expected absent on stock
  firmware and hidden permanently, not "coming later").

### 3.2 Expert Mode

Expert Mode adds:

- target material identity and Creality material-code value;
- codec identity and version (AES-128, block layout, ECB vs CBC — see §8.2
  for the open empirical question);
- mapping confidence and evidence;
- density, diameter, mass-category and calculated nominal length;
- generated serial and compact date/batch data;
- masked tag UID and the AES-derived Key B (masked in exports, per §16);
- block verification status;
- raw Blocks 4–7 when read access is available;
- experimental mapping override;
- Moonraker endpoint configuration and object browser;
- `box` object inspection and the documented `M8200` load/unload command set
  (§11) — **not** `BOX_LOAD_MATERIAL`, which is known-buggy on K2 firmware;
- reviewed macro execution in a later hardware-validated release;
- exportable sanitised diagnostics.

Expert Mode is enabled only after a safety acknowledgement. It is a
presentation and governance boundary, not a substitute for device
authentication.

## 4. Main navigation

- **Home** — status, main action, device-compatibility indicator (§8.5),
  printer connection shortcut.
- **Tag** — guided spool preparation and NFC transaction.
- **Spools** — local inventory and tag history.
- **Printer** — Moonraker/`box` probing and capability-led monitoring.
- **Settings** — Simple/Expert mode, privacy, application information.

Edge-to-edge rendering, Material 3, dynamic colour on Android 12+, adaptive
layouts, large touch targets, predictive-back-compatible activity foundations.

## 5. Filament catalogue

The bundled catalogue draws on SpoolmanDB for filament facts (manufacturer,
material, density, diameter) and maintains a **separate** governed CFS
mapping layer for the Creality material code, because SpoolmanDB data is not
itself proof of a Creality material identity.

Each filament product record contains: canonical ID; manufacturer; product
line; material composition label; density; standard mass category; diameter;
colour list; target Creality material code; mapping confidence; source URL.

## 6. Mapping confidence

Mappings are classified as:

- **Verified** — reproduced against a captured golden vector or real hardware
  read-back on identified printer/CFS/firmware and tag hardware.
- **Strong** — supported by several independent community observations.
- **Reasonable** — generic material-family fallback.
- **Experimental** — plausible but not sufficiently tested.
- **Unsupported** — no safe mapping.

Simple Mode may not silently use Experimental or Unsupported mappings. Expert
Mode may select an Experimental mapping only after a visible warning.

As of this revision, the *cryptographic* layer (key derivation, block
encryption, payload framing) is Verified for the standard case (see §8) and
is no longer the source of "Experimental" ratings; a filament earns an
Experimental rating only when its **material code** or **colour-to-swatch
mapping** is unconfirmed against the Creality material database, not because
the write mechanism itself is in doubt.

## 7. Length, mass and the quantised-weight correction

For filament mass `m`, density `rho` and diameter `d`, nominal length is:

`L(m) = m(g) × 1000 / [rho(g/cm³) × π × (d(mm)/2)²]`

**Correction from the prior revision:** the CFS tag does not store an exact
mass. It stores a fixed weight *category* (documented values include 250 g,
500 g, 600 g, 750 g and 1 kg). The tag records the **original spool weight at
tagging time**, not remaining quantity. The application must therefore:

- treat the tag's stored length/weight field as an **identity fact**
  (`nominal starting quantity`), never as a live consumption number;
- source live remaining quantity from `box.remain_len` (the CFS's own
  per-slot estimate, in metres — see §11) when a printer is connected, and
  from Moonraker's Spoolman integration (§13) for authoritative
  weight-based remaining stock;
- only fall back to a phone-calculated estimate (this formula) when no
  printer/Spoolman connection exists, and label it clearly as an estimate.

For 1 kg PETG at 1.27 g/cm³ and 1.75 mm diameter, the calculated nominal
length is approximately 327 m — this remains correct as a *starting-quantity*
estimate, not as a live-remaining number.

### 7.1 Material code mapping

The Creality material identity is a `1XXXXX`-style code (leading digit is a
tag-format marker, stripped on read) referencing entries in Creality's
cloud-fetched material database. Approximately 56 material codes are known
from community documentation (PLA/PETG/ABS/ASA/TPU/PA/PC/PPS families and
variants). The mapping table is maintained as data, versioned and updated
independently of app releases, and is the correct home for "Experimental"
ratings going forward (§6).

## 8. RFID architecture — corrected

### 8.1 What is actually encrypted, and why "Simple Mode hides encryption" no longer means "hides an unknown secret"

The CFS tag is a MIFARE Classic 1K tag. The identity payload lives in
**Sector 1, Blocks 4–6**, as a 48-byte ASCII string, encrypted per-block with
AES-128 (see §8.2 for the ECB/CBC question) under a key that is **not**
random or vendor-secret in the sense the prior revision assumed. It is
**deterministically derived from the tag's own 4-byte UID**: the UID is
processed through AES using a fixed firmware master key, and the result is
truncated to 6 bytes to form MIFARE sector authentication **Key B**. Given
any tag's UID, Key B is computable offline with no attack, dictionary, or
brute-force required.

This is not a novel finding of this project — it is documented and
reproducible in the open-source community, with a published golden vector
(UID `35B94A19` → Key B `239E7FE23653`, decrypting to a payload identifying
HyperPLA, material code `01001`, length code `0330` = 1.0 kg, serial
`000001`, colour black). **This golden vector is the mandatory unit-test
fixture for any codec implementation in this app (§8.4).**

Practical consequence: the prior spec's framing of H-001 ("no Crypto1
attack tooling, no supplied keys, therefore write is blocked") is retired.
Simple Mode still hides the concept of "keys" and "encryption" from the user
for UX reasons — it is not hiding an unsolved secret, it is hiding a solved
mechanism the user does not need to understand.

### 8.2 Open empirical question: ECB vs CBC

Community sources disagree on block-cipher mode. The DnG-Crafts lineage
(and its ports, including the Kotlin reference this app forks from) implies
AES-128-ECB. At least one independent implementation claims AES-128-CBC with
a zero IV. Because ECB and CBC produce identical ciphertext for the *first*
16-byte block of a message but diverge from the second block onward, this
must be resolved empirically against the golden vector (§8.1) before any
write path ships: decrypt Blocks 5 and 6 under both modes and confirm which
reproduces the known-good plaintext. This is a same-day bench task once a
codec is forked, not a research blocker.

### 8.3 Fork, don't rebuild

The correct action is to **fork an existing implementation**, not write a new
codec:

- Preferred: fork the Kotlin/Jetpack Compose Android app that already
  implements this exact codec, OCR-based label scanning, a 42-colour palette
  plus eyedropper, and 56+ material types from the Creality firmware
  database, in the same stack this project uses (Kotlin/Compose, Material
  3). Its licence permits forking.
- Reference: a Python port of the canonical upstream reverse-engineering
  project is useful as a second, independently-written implementation to
  diff the ported Kotlin code against, and is the source of the golden test
  vector in §8.1.
- Canonical upstream: the original K2/K1/Hi/CFS RFID programming project
  (Android/Arduino/Windows sources, an existing published Play Store app,
  and Spoolman integration already built in) is the most-maintained lineage
  and the ultimate reference if the Kotlin fork and Python port disagree.
- Fallback hardware path: if phone NFC support proves too inconsistent
  (§8.5), an ESP32 + PN532 companion device running a web-app-controlled
  firmware based on the same canonical upstream is a proven alternative that
  moves the write operation off the phone's NFC controller entirely, with
  the Android app becoming its web/companion UI.

App code for this subsystem should be limited to: adapting the forked
codec's package structure to this app's architecture, wiring it to the
Simple/Expert UX and mapping-confidence model above, and adding the
device-capability gate below. The AES routine, block layout, and key
derivation are not to be re-derived from scratch.

### 8.4 Golden-vector regression test (mandatory)

Before any release enables the write path:

1. Implement or import the codec.
2. Run it against UID `35B94A19` and confirm Key B derives to `239E7FE23653`.
3. Decrypt the known ciphertext for Blocks 4–6 and confirm the plaintext
   matches the documented HyperPLA/`01001`/`0330`/`000001`/black record
   exactly.
4. Resolve §8.2 (ECB vs CBC) as part of this same test if the ported
   reference doesn't already pin it.

A codec that fails this test must not be wired to the write UI.

### 8.5 Device-capability gate (H-000 — new top risk, promoted from a footnote)

`android.nfc.tech.MifareClassic` support is **optional** on Android and
implemented only where the device's NFC controller is NXP or
NXP-compatible. Devices with Broadcom or certain other controllers can
enumerate `NfcA` (UID only) but cannot authenticate sectors or write blocks
at all — `MifareClassic` will simply never appear in `getTechList()`. There
are also documented cases of newer flagship phones (e.g. certain Pixel 8/8
Pro units) failing MIFARE Classic operations with non-default keys despite
having NXP-class hardware, and iOS is unsupported entirely because Apple
restricts MIFARE Classic access at the OS level (this remains an
Android-only app).

**Requirement:** on first launch and before entering any write flow, the app
must:

1. Enumerate `getTechList()` on any scanned tag and confirm `MifareClassic`
   is present, not just `NfcA`.
2. Maintain and consult a maintained allow/deny list of known-good and
   known-bad devices, seeded from the community-maintained compatibility
   lists that accompany the MIFARE Classic Tool project, and from this
   project's own fork's stated known-good devices (Galaxy S25/S26, Pixel
   family observed working).
3. Refuse to enter the write flow with a clear, specific message
   ("this phone's NFC hardware does not support the tag type this app
   needs") **before** attempting a write — never fail mid-transaction and
   report a false "unsupported tag" error to the user.

This gate is Simple Mode's first screen concern, not an Expert Mode detail:
an unsupported phone is the single most common reason this app will not
work for a given user, and it must be diagnosed immediately, not after a
failed write.

### 8.6 What the generic writer does and does not touch

The service probes a tag, verifies `MifareClassic` capability and 1K
capacity, authenticates Sector 1 using the derived Key B, reads and writes
Blocks 4–6, and performs an immediate read-back comparison. It does not
modify Block 7 (the sector trailer) on tags that already carry factory
security settings; writing a fresh sector trailer on a genuinely blank tag
(as opposed to a re-used factory CFS tag) is a separate, hardware-verified
procedure (§20) and is not assumed identical to the re-tagging case.

## 9. Tag transaction sequence

1. Wait for NFC tag.
2. Run the device/tag capability gate (§8.5). Abort with a clear message if
   unsupported.
3. Verify `MifareClassic` technology and 1K capacity.
4. Read UID; derive Key B (§8.1).
5. Resolve codec version and material mapping.
6. Authenticate Sector 1.
7. Back up Blocks 4–7.
8. Construct all three output blocks before writing.
9. Validate exact block size.
10. Write Block 4; read and compare.
11. Repeat for Blocks 5 and 6.
12. Decode the final stored data.
13. Compare semantic values with the requested spool.
14. Record success only after all checks pass.

If a tag is removed, authentication fails, or read-back differs, the
application reports failure and retains a recovery record. It never infers
success from a write call returning without exception.

## 10. Second-tag workflow

Unchanged from the prior revision: many CFS spool configurations use two
matching physical tags carrying the same logical spool identity, material,
colour, quantity and serial. The application records two physical tag
references under one spool record.

## 11. Printer integration — corrected to the documented K2 `box` object

Printer integration is optional and read-mostly. The app queries a
Moonraker-compatible endpoint for the `box` object, which — per current
community reverse-engineering of the K2 series — returns, per CFS unit
(T1–T4): `color_value` (format `0RRGGBB`, strip the leading digit),
`material_type` (format `1XXXXX`, strip the leading digit — see §7.1),
`remain_len` (metres remaining, live), `temperature`, `dry_and_humidity`,
`state`, plus top-level `auto_refill` and `same_material` (the auto-refill
grouping) and `map` (slot layout).

**Load/unload commands use the `M8200` macro family**
(`P`/`L I={slot}`/`C`/`R`/`F`/`O` parameters), not `BOX_LOAD_MATERIAL`, which
is documented as buggy on K2 firmware (it omits a required pre-operation
step and can crash the print process). Expert Mode must expose `M8200`
options, not `BOX_LOAD_MATERIAL`.

Chamber heating is via `M141 S{temp}` (coordinating a
`heater_generic chamber_heater` and a `temperature_fan chamber_fan`
internally): `S0` = off, `S` at or below the low threshold maintains via fan
only, above it drives active heating. Camera is served over **WebRTC on port
8000** of the printer; do not hardcode this — confirm it live (§14).

**Important caveat:** the detailed `box`/`M8200`/error-code documentation
this section relies on has been confirmed against the **K2 Plus**. The K2
Pro — this project's actual target — is explicitly marked untested in the
same source. Treat the object names and command set as high-probability, not
verified, until confirmed against real K2 Pro hardware (§20).

Note also that CFS control on the printer itself runs through a closed-source
compiled component, and the printer's own display communicates with the CFS
directly over a serial connection that bypasses the normal command path for
load/unload. This means an Expert Mode G-code or `M8200` command issued from
this app can leave the printer's own screen showing stale state. Any such
command must carry a visible warning to that effect.

Planned trust levels (read-only, standard control, filament control, expert
control, developer control) are retained from the prior revision; add an
explicit acknowledgement, at the "expert control" tier and above, that the
stock display may desync from app-issued CFS commands.

### 11.1 Recommended deployment: SpoolmanSync, not an app-built dashboard

A ready-made, MIT-licensed, Docker-deployable tool — **SpoolmanSync**
(gibz104/SpoolmanSync) — already provides a full CFS slot/tray dashboard,
click-to-assign spool matching, automatic per-print weight deduction, low-
stock alerts, and QR/Web-NFC spool assignment, built on `ha_creality_ws` +
Home Assistant + Spoolman, with explicit K1/K2/K2 Plus/Hi/Ender-3-V3-CFS
support. It has three install modes, including a no-YAML "embedded" mode
(bundled Home Assistant in one `docker compose up`) for users without an
existing HA install.

This changes the app's own printer-facing ambition: the Printer tab should
**not** attempt to reproduce a slot dashboard, consumption view, or low-
stock UI. Once a tag is written, SpoolmanSync (pointed at the same
Moonraker/`ha_creality_ws` stack) is the better home for all of that —
richer, already tested by its own userbase, and zero app code. The app's
Printer tab is reduced to:

- basic online/offline + temperature readout (retained, trivial, useful
  even with SpoolmanSync running);
- a native auto-refill toggle (retained — this is a one-line printer
  command, not a dashboard, and there's no reason to make the user leave
  the app to flip it);
- a link/pointer to the user's SpoolmanSync instance for everything else
  (slot view, consumption, low-stock alerts), rather than an in-app
  reimplementation.

Raw `box` object inspection remains available in Expert Mode for
diagnostics (confirming K2 Pro's actual field names against §11's K2-Plus-
sourced documentation, per H-004), but is no longer positioned as a Simple
Mode feature.

**Important distinction:** SpoolmanSync's "RFID auto-match" and Web-NFC
spool assignment *consume* a tag that already carries a recognisable
identity — it does not write Creality's proprietary encrypted MIFARE
Classic payload onto a blank third-party spool. Writing that tag remains
this app's unique job (§8-§9); SpoolmanSync is downstream of it, not a
substitute for it.

## 12. Spool relay — confirmed as a native feature, not an app responsibility

Automatic continuation from a depleted spool to a compatible backup spool is
a **native CFS firmware feature** (documented as "automatically switch to a
filament with the same type and color" when a slot runs out), toggled via
`BOX_ENABLE_AUTO_REFILL` and reflected in the `box.auto_refill` /
`box.same_material` fields (§11). This confirms the prior revision's design
instinct — do not run relay logic in the Android app — and removes the need
to design a custom relay policy engine: the app's job is to **surface and
toggle the native setting**, and to display the resulting slot grouping, not
to implement matching logic itself.

Simple Mode: a single toggle, "auto-refill same material/colour," reflecting
and setting the native feature.

Expert Mode: full visibility into which slots are currently grouped as
mutual backups (`same_material`), with the ability to override which slots
are eligible.

The Strict/Compatible/Manual-approved matching-tier design from the prior
revision is retired as unnecessary — the printer already does exact
same-type/same-colour matching natively. If Creality firmware later exposes
looser matching, revisit.

## 13. Chamber thermal safeguards

Unchanged in principle from the prior revision (advisory-only in the app;
active policy is printer-side), now grounded in specific mechanisms:

- Advisory comparison uses the printer's actual chamber sensor state versus
  the selected filament's known-safe range, not a hardcoded material→
  temperature table.
- Active policy, if ever implemented, should build on existing printer-side
  primitives — `M141`/`M191`-style macros and Klipper's own heater
  fault/shutdown watchdog mechanism (`verify_heater`) — rather than a
  bespoke Android-side thermal controller. This is a macro/config package to
  install on the printer, not app code.
- No universal rule ("all ASA at 60°C") is accepted; the correct chamber
  target depends on exact product, printer configuration, sensor placement,
  firmware and geometry.

## 14. Consumption accounting — delegated to Moonraker's Spoolman integration

**Correction from the prior revision:** Moonraker already implements
job-level, idempotent Spoolman consumption tracking natively via its
`[spoolman]` component and `server.spoolman.post_spool_id` API, returning
authoritative `remaining_weight`/`used_weight` figures. The app does not
need to build its own accounting lifecycle, idempotency-key scheme, or
length-to-mass conversion pipeline for the authoritative number — it
**configures** the Moonraker/Spoolman connection and **displays** the
result.

Multi-slot attribution (which physical spool in which CFS slot maps to
which Spoolman spool record) uses the tag's own serial number as the
Spoolman spool ID — a convention already demonstrated in community tooling
for the related Creality Hi platform. The app should write this
serial-to-spool-ID convention into the tag at tagging time (§9) so that any
Moonraker/Spoolman-side tooling can attribute usage without a separate
mapping step.

As of §11.1, the app's job here ends at writing that serial into the tag.
**SpoolmanSync** is the recommended deployment for everything downstream:
its "RFID auto-match" mode is built to recognise exactly this kind of
tagged CFS spool and keep Spoolman's weight tracking in sync automatically,
including low-stock alerts. The app should not build its own consumption
display, idempotency-key ledger, or low-stock UI — that would duplicate a
tool that already exists, is tested, and is one `docker compose up` away.

The phone-side length→mass formula (§7) is retained only as an offline
fallback estimate when no Spoolman/SpoolmanSync connection exists, clearly
labelled as such. Per-layer or per-print phone-side deduction is not
implemented, in the app or otherwise — that is SpoolmanSync's job.

## 15. Camera and telemetry

**Camera:** served natively over WebRTC on port 8000 of the printer (§11).
The app should discover/confirm this endpoint at runtime rather than
hardcoding it, and can model its embedding approach on an existing
Home-Assistant integration for Creality printers that already wraps this
exact camera path (including K2 Pro) via a bundled or external WebRTC
relay component — useful as a reference implementation even though this app
embeds the stream directly rather than through Home Assistant.

**Closed-loop step-servo telemetry / collision detection:** the K2's
extruder is a closed-loop FOC servo, and Creality surfaces its faults as
printer/UI error codes (documented examples include peak-current-protection
and extruder-error codes), not as Moonraker objects exposing per-axis
following-error, motor current, or driver-fault fields. The
`MotionTelemetryCapabilities` probe from the prior revision is retained as a
mechanism, but the expected and default UI state is: **this feature is
absent on stock K2 firmware, and the UI should treat it as permanently
unavailable rather than "pending hardware validation."** If a future
firmware or community Klipper fork exposes genuine per-axis telemetry, the
capability probe will detect it and the UI can be re-enabled — but this is
not something to design toward as an expected near-term feature.

The app must never infer a collision or fault from indirect signals (current
change, velocity change) — only from a documented, printer-reported fault
code or a proven firmware-specific telemetry object.

## 16. Privacy

Unchanged from the prior revision: no account, no advertising, no analytics
SDK, no central telemetry, local-only spool data, local printer URL storage,
no automatic image upload, no upload of tag UID, keys or block data.
Production credentials stored with Android Keystore-backed encryption.
Diagnostic exports mask UIDs, remove credentials, and exclude the derived
Key B value even though it is not a secret in the traditional sense — mask
it anyway, since a leaked UID+KeyB pair still identifies a specific physical
tag.

## 17. Accessibility and UX

Unchanged from the prior revision: edge-to-edge layouts, dynamic system
colour, dark/light mode, semantic icons plus text, 48dp+ touch targets,
clear disabled-state explanation (particularly for the §8.5 device gate —
"disabled because this phone can't do X" beats a generic greyed-out button),
large typography hierarchy, no colour-only status, TalkBack-ready semantics,
progressive disclosure, explicit safety acknowledgement for Expert Mode.

## 18. Acceptance criteria

A release is complete only when:

- the codec passes the golden-vector regression test (§8.4) with the
  ECB/CBC question resolved;
- the device-capability gate (§8.5) correctly refuses write flows on at
  least one confirmed-incompatible device and correctly proceeds on at
  least two confirmed-compatible devices;
- a real K2 Pro/CFS combination recognises written tags;
- a failed write never appears successful;
- Simple Mode requires no protocol knowledge;
- Experimental material-code mappings are blocked in Simple Mode;
- native auto-refill toggling (§12) and basic online/temperature status
  (§11.1) work against a real K2 Pro, not just the K2 Plus reference data;
  full slot/consumption display is intentionally NOT app scope — SpoolmanSync
  (§11.1) covers it, and its own compatibility with a real K2 Pro is that
  project's acceptance criterion, not this one's;
- all third-party code/data forked or depended upon is attributed in
  `THIRD_PARTY.yml` with licence and commit recorded;
- Play target API and privacy declarations are current;
- release AAB is signed and Play pre-launch checks pass (if ever
  distributed beyond personal/GitHub use);
- every remaining hardware-dependent open item in §20 is either closed or
  visibly and permanently absent from the UI (not "coming soon").

## 19. Reuse map — what to fork instead of build

| Subsystem | Do not build from scratch — reuse | Integration approach |
|---|---|---|
| CFS tag codec (keys, AES, block layout) | Existing Kotlin/Compose Android app implementing this exact codec | Fork directly; adapt package structure to this app |
| Codec cross-check / golden vector | Existing Python port of the canonical upstream reverse-engineering project | Use as an independent reference and the source of the §8.4 test fixture |
| Canonical upstream reference | Original Android/Arduino/Windows K2/K1/Hi/CFS RFID project | Consult when the Kotlin fork and Python port disagree; has an existing Spoolman-integrated Play Store app as a working example |
| ESP32 hardware fallback | Existing web-app-controlled ESP32+PN532 firmware based on the same canonical upstream | Adopt wholesale if phone NFC support (§8.5) proves too inconsistent |
| Spool relay on runout | Native CFS `BOX_ENABLE_AUTO_REFILL` firmware feature | App only toggles the setting (§12); no dashboard, no relay logic in-app |
| CFS slot dashboard, consumption tracking, low-stock alerts | **SpoolmanSync** (gibz104/SpoolmanSync) — Docker-deployable, MIT, built on `ha_creality_ws`+Spoolman, explicit K1/K2/K2 Plus/Hi/Ender-3-V3-CFS support | Deploy alongside Moonraker; app links out to it (§11.1) instead of building its own slot/consumption UI |
| CFS-slot ↔ Spoolman-spool attribution | Existing serial-number-as-spool-ID convention from community Creality-Hi/Spoolman tooling, also matched by SpoolmanSync's "RFID auto-match" | Adopt the convention; write serial into tag at tagging time — SpoolmanSync consumes it from there |
| Camera | Native printer WebRTC:8000 endpoint | Discover/confirm at runtime; consult existing Home-Assistant Creality integration's approach for reference |
| Printer/CFS object schema | Community-documented `box` object + `M8200` command reference | Query and parse per that schema; explicitly avoid `BOX_LOAD_MATERIAL` |
| Chamber thermal watchdog | Existing Klipper macro packages plus Klipper's own `verify_heater` fault mechanism | Install printer-side; app stays advisory-only |
| Device NFC-compatibility data | Community-maintained MIFARE-Classic-Tool compatible/incompatible device lists | Seed the §8.5 allow/deny list; keep updatable as data, not hardcoded |

Net-new app code is reserved for: the Simple/Expert UX layer, the
device-capability gate (§8.5), the mapping-confidence/catalogue governance
layer (§6–§7.1), and glue wiring the above into a coherent Compose UI.

## 20. Open items requiring physical K2 Pro / CFS / tag hardware

These cannot be closed by research alone:

- Confirm the K2 **Pro** (not just Plus) exposes the same `box` object
  fields and `M8200` behaviour documented in §11.
- Confirm the exact sector-trailer/access-bits write sequence for a
  genuinely blank tag versus a re-used factory CFS tag (§8.6) — the codec
  fork's documented flow suggests factory tags already carry a trailer that
  should not be touched, while blank tags need one written using the
  derived Key B, but this needs bench confirmation.
- Confirm the CFS actually accepts app-written tags after a Creality
  firmware update — community reports describe firmware updates breaking
  previously-working custom tags.
- Confirm multi-slot Spoolman attribution end-to-end (§14) with real CFS
  slots, using the serial-as-spool-ID convention.
- Confirm whether any genuine motion/servo telemetry is exposed on K2 Pro
  firmware (§15) — expected answer is no, to be confirmed rather than
  assumed.
- Resolve the ECB-vs-CBC question (§8.2) empirically against the golden
  vector.
- Determine actual write reliability across two or more real Android NFC
  controller families (§8.5), not just documentation-based compatibility
  lists.
