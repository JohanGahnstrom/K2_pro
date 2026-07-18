# Model and print workflow

## Decision

OpenFilamentCFS may download and retain model assets, but a K2 Pro executes printer-ready G-code rather than STL/OBJ/3MF/GLB geometry. The first implementation therefore provides:

- Android Storage Access Framework import;
- direct HTTP/HTTPS download;
- SHA-256 calculation;
- explicit file-type classification;
- Moonraker multipart upload with checksum verification;
- optional upload-and-start;
- per-printer destination selection.

Source geometry is accepted into the local model workspace but cannot be sent as printable data. The UI marks it as requiring slicing.

## Reuse choices

- Moonraker file and print APIs are the protocol authority.
- OctoApp is the strongest Android workflow reference for connect/prepare/print, but the OpenFilament product remains K2 Pro/CFS-specific.
- Google Filament is the preferred future Android preview renderer for glTF/GLB.
- PrusaSlicer/OrcaSlicer remain external slicing engines until a validated service or native integration is proven.

## Safety

Upload-and-print is enabled only for recognized G-code extensions and a selected configured printer. Model geometry cannot bypass slicing validation.
