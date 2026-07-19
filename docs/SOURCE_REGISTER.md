# Direct System and Research Sources

Version: 0.2.0 — revised alongside FUNCTIONAL_DESCRIPTION.md 0.2.0.

## Android

- Android 16 behaviour changes: https://developer.android.com/about/versions/16/behavior-changes-16
- Android 16 features: https://developer.android.com/about/versions/16/features
- Google Play target SDK requirement: https://developer.android.com/google/play/requirements/target-sdk
- Android MIFARE Classic API (note the "implementation is optional" caveat — this is now central to H-000, not a footnote): https://developer.android.com/reference/android/nfc/tech/MifareClassic
- Advanced NFC overview: https://developer.android.com/develop/connectivity/nfc/advanced-nfc
- Activity/Compose edge-to-edge and predictive back releases: https://developer.android.com/jetpack/androidx/releases/activity
- Material Design 3: https://m3.material.io/
- MIFARE Classic Tool compatible/incompatible device lists (seed data for the H-000 allow/deny list):
  https://github.com/ikarus23/MifareClassicTool/blob/master/COMPATIBLE_DEVICES.md
  https://github.com/ikarus23/MifareClassicTool/blob/master/INCOMPATIBLE_DEVICES.md
- Community reports of MIFARE Classic failures on modern flagship devices (background for H-000, treat as anecdotal not authoritative): https://xdaforums.com/t/nfc-mifare-classic-1k-not-working-on-pixel-8-8-pro.4679605/

## Printer and Moonraker

- Moonraker API introduction and WebSocket: https://moonraker.readthedocs.io/en/latest/external_api/introduction/
- Moonraker printer administration, query and subscriptions: https://moonraker.readthedocs.io/en/latest/external_api/printer/
- Moonraker printer objects: https://moonraker.readthedocs.io/en/latest/printer_objects/
- Moonraker third-party integrations, including the `[spoolman]` component (resolves H-007): https://moonraker.readthedocs.io/en/latest/external_api/integrations/
- Klipper API server: https://www.klipper3d.org/API_Server.html
- Klipper heater configuration and the `verify_heater` watchdog mechanism (basis for any future H-006 work): https://www.klipper3d.org/Config_Reference.html#heaters
- **HelixScreen K2-series developer documentation (new — the single best K2 `box` object / M8200 / WebRTC:8000 / error-code reference found; confirmed on K2 Plus, K2/K2 Pro marked untested):** https://helixscreen.org/dev/printers/creality-k2/
- Creality Wiki — K2 Plus automatic-refill guide (confirms native relay feature, H-005): https://wiki.creality.com/en/k2-flagship-series/k2-plus/automatic-refill-guide
- Creality Wiki — CM2783 error code (extruder peak-current-protection; supports H-009 finding that faults are error codes, not telemetry objects): https://wiki.creality.com/en/printers-general-documents/CM2783

## Community CFS/RFID projects

- DnG-Crafts K2-RFID — canonical, most-maintained upstream (Android/Arduino/Windows sources, existing Play Store app, built-in Spoolman integration): https://github.com/DnG-Crafts/K2-RFID , releases at https://github.com/DnG-Crafts/K2-RFID/releases , Play Store app: https://play.google.com/store/apps/details?id=dngsoftware.spoolid
- **sybethiesant/CFSWriter (new — preferred fork target: Kotlin/Compose/Material 3 Android app implementing the full codec, OCR label scanning, 42-colour palette + eyedropper, 56+ material types):** https://github.com/sybethiesant/CFSWriter
- **flamebarke/creality_rfid (new — Python3 port of DnG-Crafts; source of the golden test vector used in FUNCTIONAL_DESCRIPTION.md §8.4):** https://github.com/flamebarke/creality_rfid
- soylentOrange K2-RFID — ESP32 + PN532 hardware fallback path, web-app-controlled: https://github.com/soylentOrange/K2-RFID
- lot38designs "RFID for CFS" — fork of MifareClassicTool, Play Store app: https://play.google.com/store/apps/details?id=com.lot38designs.cfsrfid&hl=en
- ikarus23/MifareClassicTool — now primarily useful as the device-compatibility reference (see Android section above): https://github.com/ikarus23/MifareClassicTool
- **MakaiView/cfs-programmer (new — ESP32-S3 + PN532 + Mac app; claims AES-128-CBC with zero IV, conflicting with the ECB majority — see FUNCTIONAL_DESCRIPTION.md §8.2):** https://github.com/MakaiView/cfs-programmer
- CFSync — printer-side Klipper/Moonraker CFS slot + Spoolman sync service: https://github.com/koen01/CFSync
- **ityshchenko/klipper-cfs (new — early-stage community Klipper CFS module; README states "not ready for production use," track but don't depend on yet):** https://github.com/ityshchenko/klipper-cfs
- Mobileraker — Flutter Moonraker/Klipper client with camera support, useful UX/architecture reference: https://github.com/Clon1998/mobileraker
- jschuh Klipper macros (M141/M191 chamber macros directly relevant to §13): https://github.com/jschuh/klipper-macros
- **CrealityOfficial/K2_Series_Klipper (new — official but incomplete Klipper fork with CFS/RFID binary blobs):** referenced via Creality's own GitHub org
- **Guilouz/Creality-K2Plus-Extracted-Firmwares (new — extracted stock firmware images, useful for confirming object names/behaviour without live hardware):** referenced via GitHub

## Payload field research (2026-07-19)

- **batch/supplier field semantics** — MainViewModel.writeTag hardcodes
  `batch = "1A5"` and `supplier = "1B3D"` (the golden vector's own literal
  values) for every write, which reads as a placeholder worth questioning.
  Checked flamebarke/creality_rfid's README
  (https://raw.githubusercontent.com/flamebarke/creality_rfid/main/README.md)
  directly: its documented `pm3write` CLI only exposes `--material --color
  --length` (batch/date/supplier are not CLI-configurable in the shown
  usage), and its write example's logged output still shows the exact same
  Batch/Supplier as the golden vector. That's suggestive that this app's
  current behaviour matches the reference tool's own demonstrated usage,
  not proof — the actual `.py` source could not be located (no directory
  listing available over any host this session could reach: github.com and
  api.github.com are blocked by this delivery environment's network
  policy, and targeted path guesses on raw.githubusercontent.com for the
  source file all 404'd). Treat as inferred, not bench-confirmed, same as
  the ECB/CBC and blank-tag-trailer open items.

## Printer-side automation and integration (new section)

- **3dg1luk43/ha_creality_ws (new — Home Assistant WebSocket integration; K2/K2 Pro camera via go2rtc, box temp control, power; open feature-request issue for CFS status, #50):** https://github.com/3dg1luk43/ha_creality_ws , README: https://github.com/3dg1luk43/ha_creality_ws/blob/main/README.md , issue: https://github.com/3dg1luk43/ha_creality_ws/issues/50
- **pickmanmike/creality-hi-spoolman-tools (new — demonstrates the serial-number-as-Spoolman-spool-ID convention adopted in FUNCTIONAL_DESCRIPTION.md §14):** https://github.com/pickmanmike/creality-hi-spoolman-tools
- **gibz104/SpoolmanSync (new, high priority — MIT, Docker-deployable CFS/AMS slot dashboard, automatic per-print weight deduction, low-stock alerts, RFID/QR/Web-NFC spool assignment; explicit K1/K2/K2 Plus/Hi/Ender-3-V3-CFS support via ha_creality_ws + Spoolman; three install modes incl. a no-YAML bundled-HA "embedded" mode):** https://github.com/gibz104/SpoolmanSync . This is the recommended deployment for everything downstream of tag-writing — see FUNCTIONAL_DESCRIPTION.md §11.1. It reads/matches tags that already carry an identity; it does not write Creality's encrypted CFS payload onto a blank tag, so it complements rather than replaces this app.

## Researched but not adopted (recorded for completeness)

- **spuder/OpenSpool** — https://github.com/spuder/OpenSpool . ESP32+PN532 NFC reader/writer for filament RFID, using NTAG215/216 + an NDEF/JSON protocol distinct from Creality's MIFARE-Classic/AES scheme. Its own README lists Creality (MIFARE Classic 1K) support as "planned," not implemented, and Klipper/Moonraker integration as "planned" too — its one proven path is Bambu over MQTT. Not code-reusable for this project's codec, but a second working example of the ESP32+PN532 hardware pattern, useful as a fallback-writer reference if phone NFC compatibility (H-000) proves too inconsistent.
- **DMontgomery40/mcp-3D-printer-server** — https://github.com/DMontgomery40/mcp-3D-printer-server . An MCP server letting an LLM control OctoPrint/Klipper-Moonraker/Duet/Repetier/Bambu/Prusa/Creality-Cloud printers plus STL manipulation/slicing. No RFID, CFS, or Spoolman support. Not applicable to this Android app; potentially useful to the developer personally as a separate operator tool for querying/controlling a Moonraker-connected printer from Claude.
- **makermate/claw3d-skill** — https://github.com/makermate/claw3d-skill . A Claude/OpenClaw agent skill for AI-generate → search (Thingiverse) → slice → print workflows, with a Moonraker printer backend. Unrelated to filament identity, CFS, or Spoolman. Not applicable to this project.

## Filament catalogue and inventory

- SpoolmanDB repository: https://github.com/Donkie/SpoolmanDB
- SpoolmanDB compiled catalogue: https://donkie.github.io/SpoolmanDB/filaments.json
- Spoolman repository: https://github.com/Donkie/Spoolman
- Spoolman documentation: https://donkie.github.io/Spoolman/
- Spoolman wiki — Automatic Filament Usage Tracking (documents the Moonraker `[spoolman]` integration relied on in H-007): https://github.com/Donkie/Spoolman/wiki/Automatic-Filament-Usage-Tracking
- **SimplyPrint helpdesk — "The Creality material standard: NFC/RFID for the Creality CFS" (new — good plain-English confirmation of tag format, AES/UID-derived keys, quantised weight buckets, ~56 material types, no-iOS caveat):** https://help.simplyprint.io/en/article/the-creality-material-standard-nfcrfid-for-the-creality-cfs-1crrofa/

## Deprecated / do not rely on

- `Creality-Laser/K2-RFID` — appears dead (404) as of this research pass; previously cited via SimplyPrint. Removed from active source list.

## Important source-use rule

These links are research and interoperability sources. They are not all
licence-compatible with every possible application licence — though for
this project's current personal/GitHub-published use, licence compatibility
is not a blocking constraint. A reverse-engineered observation is not
automatically valid for all firmware versions, and several of the above
sources are confirmed on the K2 Plus rather than the K2 Pro this project
targets (see H-004, H-011 in HOLD_REGISTER.md). Before copying code, record
its licence, commit and affected files in `THIRD_PARTY.yml` regardless.
