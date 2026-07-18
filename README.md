# OpenFilamentCFS

OpenFilamentCFS is a focused Android application for creating and verifying Creality CFS-compatible MIFARE Classic 1K filament tags.

## Product boundary

The phone performs the task that uniquely requires a phone in hand:

1. validate MIFARE Classic capability;
2. build the CFS spool identity;
3. write the tag;
4. convert a blank default-key tag when required;
5. read back and verify every written block.

The app deliberately does not duplicate persistent services:

- CFS tray assignment, print deductions and low-stock alerts: SpoolmanSync;
- spool inventory: Spoolman;
- runout relay: native CFS auto-refill;
- thermal protection: printer firmware/Klipper;
- camera and printer dashboard: existing printer interfaces.

A read-only K2/Moonraker probe remains available in Expert mode solely for compatibility diagnostics.

## Upstream reuse

- DnG-Crafts/K2-RFID: UID-derived keys, payload encryption and blank-tag conversion sequence.
- flamebarke/creality_rfid: independent golden-vector fixture.
- sybethiesant/CFSWriter: Android workflow and compatibility reference.
- ikarus23/MifareClassicTool: Android device compatibility evidence.
- ha_creality_ws: protocol behaviour adapted into a direct, optional diagnostic client; Home Assistant is not required by this Android app.
- SpoolmanSync: recommended downstream CFS/Spoolman workflow rather than code duplicated in this app.

## Build

```bash
./build.sh
```

The build script pulls the current Android SDK container and resolves Gradle/dependencies at build time. SDKs and caches are not bundled in the source archive.

## Model and G-code transfer

Version 0.4 adds local file import, direct URL download, SHA-256 verification, printer selection, Moonraker upload, and optional upload-and-start. STL/OBJ/3MF/GLB are treated as source assets requiring external slicing; only G-code is printable.


## Model preview and print preparation

Version 0.5 adds Android-native GLB preview with Google Filament, common slicer metadata extraction from G-code, and a disabled-by-default remote-slicer contract. STL/OBJ/3MF/GLTF remain source assets until sliced.
