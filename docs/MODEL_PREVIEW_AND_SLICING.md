# Model preview and slicing architecture

## Implemented

- Android-native GLB preview using Google Filament 1.71.5.
- Touch rotation and camera manipulation through Filament ModelViewer.
- G-code comment analysis for common OrcaSlicer, PrusaSlicer, and Cura metadata.
- SHA-256 integrity identity for every imported or downloaded asset.
- Direct Moonraker upload and optional print start for G-code only.

## Format rules

- GLB is the preferred mobile preview interchange format because buffers and textures are packaged into one file.
- Standalone `.gltf` is classified separately. It may reference external `.bin` buffers and textures, so the current app does not claim reliable preview unless it is packaged as GLB.
- STL, OBJ, and 3MF remain source geometry and require slicing.

## Slicing boundary

The Android process does not embed OrcaSlicer, PrusaSlicer, or CuraEngine. These are substantial native desktop engines and cannot responsibly be represented as a small Java/Kotlin dependency.

A vendor-neutral `RemoteSlicerClient` contract is included. A later service may:

1. accept a source model and selected validated K2 Pro profile;
2. invoke OrcaSlicer or PrusaSlicer CLI on a local workstation/server;
3. return G-code, metadata, preview images, and a reproducibility manifest;
4. require user approval before upload or print start.

No remote slicing endpoint is enabled by default.

## Print safety

Previewability never implies printability. Only classified G-code is accepted by the Moonraker upload-and-print boundary. Printer-profile compatibility remains a user-visible check and should later be strengthened against the selected K2 Pro capability profile.
