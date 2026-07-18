# Third-party notices and provenance

This project is designed around explicit upstream reuse rather than independent reimplementation.

- DnG-Crafts/K2-RFID — CFS tag crypto, layout and write-flow reference. Original project licence applies to adapted material.
- flamebarke/creality_rfid — golden-vector and protocol reference.
- sybethiesant/CFSWriter — MIT-licensed Android/Compose workflow reference.
- ikarus23/MifareClassicTool — GPL-3.0 compatibility and diagnostic reference; no code copied into the writer module.
- 3dg1luk43/ha_creality_ws — AGPL-3.0 protocol behaviour adapted in the optional direct diagnostic layer. The distributed project therefore retains AGPL-3.0 licensing.
- gibz104/SpoolmanSync — MIT-licensed downstream application linked/recommended but neither embedded nor copied.
- Donkie/Spoolman — external inventory service linked/recommended but not embedded.
- OpenSpool — fallback-hardware reference only; its tag codec is not used.

Each adapted source file should retain a file-level provenance note before public release, including the exact upstream commit used.

## Google Filament

- Project: google/filament
- Version adopted: 1.71.5
- License: Apache License 2.0
- Use: Android-native GLB/glTF 2.0 rendering through `filament-android`, `gltfio-android`, and `filament-utils-android`.
- OpenFilamentCFS-specific code: Compose lifecycle wrapper, local-file loading, touch forwarding, and model workflow integration.
