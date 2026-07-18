# External Review Notes: ChatGPT Output & OctoApp

This records what was checked from a ChatGPT-produced rewrite
(`OpenFilamentCFS_model_preview.zip`, package `com.marewise.openfilament`)
submitted for comparison, plus follow-up research on one of its cited
sources (OctoApp). Nothing here was merged into the app — see verdicts below.

## Method

Every claim below was checked by actually compiling and running the code
(same standard as this project's own `CfsCodecTest`), not by reading it and
judging plausibility. Kotlin snippets were compiled with `kotlinc` and
executed on the JVM; the OctoApp claim was checked by searching for its
actual source repository.

## CFS codec (`rfid/CfsCodec.kt`)

**Key derivation: correct.** It uses the same real AES master key
(`713362755E74316E71665A2870662431`) as this project's `CfsCodec.kt` — same
public source, copied correctly. Compiling and running its own golden-vector
test confirms it derives `239E7FE23653` from UID `35B94A19`, matching the
published vector.

**Payload layout: wrong, and unverified by its own test suite.** Its
`buildPlainPayload()` was compiled and fed the exact golden-vector fields
(HyperPLA / black / 1 kg / serial `000001`). It produced:

```
AB1241B3DA51010010000000033000000100000000000000
```

The confirmed-correct payload for those same fields is:

```
1A5241201B3D010010000000033000000100000000000000
```

It hardcodes a fictional `"AB124"` prefix in place of the real batch(3) +
date(5, YYMDD) fields, and inserts a spurious extra `"1"` before the
material code that does not exist in the real 48-byte layout — which shifts
every field after it out of alignment. Its `CfsCodecTest.kt` only checks key
derivation, never payload structure, so this was never caught. A tag written
with this code would have its material/colour/serial fields scrambled on
readback by real CFS firmware. **Do not port this file.**

## Direct printer client (`printer/CrealityDirectClient.kt`)

**Does not compile.** `postGcode()` embeds `\"` inside a Kotlin triple-quoted
raw string, which is a genuine syntax error, not a style issue — confirmed
by isolating that line and compiling it standalone. Beyond that, it guesses
at a `crealityWsPort = 9999` and alternate WebSocket URLs with no cited
source, and detects camera type by string-matching the model name (`"K2" →
WEBRTC`) rather than querying an actual endpoint. None of this traces to
documented evidence the way this project's `moonraker-direct.ts` does.

## What was independently correct or worth taking

- Its own hold register reaches the same conclusion this project did: slot
  dashboard, camera, and consumption/alerts are marked "removed from scope,
  use SpoolmanSync" — good cross-validation of the architecture, arrived at
  independently.
- `RemoteSlicerClient.kt` is an honest, no-op-by-default interface ("no
  slicing service configured") rather than a fabricated slicing engine —
  a reasonable extension-point pattern if remote slicing is ever added.
- Its NFC capability check skips a static device allow/deny list and does a
  live-only check — a defensible simpler alternative to this project's
  hybrid (static hint + live gate) approach.
- `ModelPreview.kt` uses Google's real Filament/`gltfio` renderer
  (Apache-2.0) for in-app GLB/glTF preview — a legitimate library if
  in-app model preview is ever wanted, though it's real added weight for
  what this project intentionally keeps as a thin client.

## OctoApp — the claim needs correcting

ChatGPT's `SOURCE_REUSE_DECISIONS.md` cites OctoApp as "the strongest mature
Android reference for a connect/prepare/print workflow over Moonraker,"
implying there is real source to learn from. Checked this directly:

**The OctoApp Android/iOS client (`de.crysxd.octoapp` on Google Play) is
closed-source commercial software** — Play Store distribution, a paid
"Premium" tier (ads/dark-theme removal), no public repository for the app
itself. Its developer's GitHub account (`crysxd`) publishes only
**OctoApp-Plugin** (AGPL-3.0, Python) — a small *server-side* OctoPrint/
Klipper companion plugin for push notifications, not the Android app.

So there is no actual client code to port or study — only the public
Connect/Prepare/Print workspace description from its store listing and
website, which is a legitimate *product-shape* reference (three workflow
stages: connect, prepare, print) but not a source-reuse target the way
`CFSWriter` or `flamebarke/creality_rfid` are. ChatGPT's framing overstated
what's actually available. Correct summary: OctoApp is evidence that a
connect/prepare/print three-stage workflow is a proven Android UX pattern
for Moonraker-class printers — nothing more than that can be reused from it.

## Bottom line

The lesson repeats from this project's own history (the 0.1.0 length-
formula bug): code that looks plausible and was never compiled or run
against a known answer is not trustworthy, regardless of which model wrote
it. Everything ChatGPT copied verbatim from a real public source was
correct; everything it wrote itself and didn't test was wrong or, in one
case, didn't compile at all.
