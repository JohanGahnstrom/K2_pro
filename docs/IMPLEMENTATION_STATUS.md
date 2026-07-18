# Implementation status

Implemented:

- writer-focused Material 3 application shell;
- hard MIFARE Classic capability gate;
- UID-derived sector key;
- 48-byte CFS payload encryption;
- blank-tag conversion;
- mandatory block-level read-back verification;
- documented CFS spool-size buckets;
- six-digit downstream spool identity;
- post-write SpoolmanSync/Spoolman handoff;
- Expert-only read-only printer probe;
- explicit removal of duplicated slot dashboard, consumption, alerts and camera UI.

Hardware validation remains:

- representative Android device matrix;
- K2 Pro acceptance of written tags;
- blank-tag supplier/access-bit variants;
- post-firmware-update acceptance;
- serial-to-SpoolmanSync matching.
