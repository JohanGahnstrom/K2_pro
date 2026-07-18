# Source and reuse decisions

## Reuse directly or adapt

### DnG-Crafts/K2-RFID

Use for deterministic sector-key derivation, AES payload encryption, tag layout and blank-tag conversion behaviour.

### flamebarke/creality_rfid

Use for independent test vectors and readable protocol verification.

### sybethiesant/CFSWriter

Use for Android UX/workflow comparison, material catalogue ideas and device behaviour—not as a second parallel codec.

### ikarus23/MifareClassicTool

Use as the compatibility evidence base and diagnostic reference.

### ha_creality_ws

Reuse protocol knowledge only in the optional direct-printer diagnostic adapter. Remove all Home Assistant entity/coordinator/service assumptions.

### SpoolmanSync

Do not embed or recreate it. Treat it as the recommended downstream service for tray assignment, usage deduction, alerts and spool matching. OpenFilamentCFS provides a handoff link and identity convention.

## Reference only

### OpenSpool

Hardware reference for ESP32+PN532. Its NTAG/NDEF protocol is not compatible with the encrypted MIFARE Classic CFS format.

## Explicitly excluded

### mcp-3D-printer-server

Printer-agent and STL-operation scope is unrelated to CFS tag creation.

### claw3d-skill

Generative 3D-model and print-agent workflow is a separate product concern.

## Model transfer and printing

### OctoApp — workflow reference, not wholesale import

OctoApp is the strongest mature Android reference for a connect/prepare/print workflow over Moonraker. We reuse its product boundary concept, but do not copy the entire application because OpenFilamentCFS is deliberately K2 Pro/CFS-specific and must retain its fleet, RFID, and provenance architecture.

### Moonraker — protocol authority

File upload, checksum validation, upload-and-print, and print start are implemented directly against Moonraker's documented APIs.

### Google Filament — future preview engine

Filament/gltfio is the preferred Apache-2.0 Android renderer for GLB/glTF preview. STL and 3MF need conversion/parsing before they can enter that renderer. Preview is a separate concern from print execution.

### PrusaSlicer / OrcaSlicer — external slicing boundary

Their slicing cores are mature but large native C++ systems. They are not embedded in this Android phase. The app imports printer-ready G-code and refuses to print unsliced source geometry.
