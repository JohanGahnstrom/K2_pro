# Functional description

## 1. Supported user outcome

A user selects a Creality material mapping, colour, original spool-size bucket and six-digit spool identity, taps a compatible MIFARE Classic 1K tag, and receives an explicit verified result.

## 2. Mandatory safety gates

- NFC hardware present and enabled.
- Android enumerates `MifareClassic` for the actual tag.
- Tag type and capacity are compatible.
- Sector authentication succeeds using either the UID-derived key or the default blank-tag key.
- All three encrypted data blocks are read back byte-for-byte.
- A newly secured tag authenticates with the derived key after trailer conversion.

## 3. Simple mode

Simple mode includes only:

- material code;
- colour;
- original spool-size bucket;
- spool identity;
- tap/write/verify state;
- downstream handoff guidance.

## 4. Expert mode

Expert mode adds:

- plaintext payload inspection;
- read-only direct printer compatibility probe;
- protocol and hardware diagnostics.

Expert mode does not enable automatic relay, consumption deduction, heater control, `M8200`, `BOX_LOAD_MATERIAL`, or arbitrary G-code.

## 5. Downstream handoff

After a verified write, the app directs users to SpoolmanSync for CFS tray assignment, print deductions and low-stock alerts, and to Spoolman for inventory. OpenFilamentCFS does not mirror those databases or keep a competing slot state.

## 6. Fallback writer

If representative-phone testing shows unacceptable MIFARE Classic incompatibility, the supported fallback architecture is an ESP32 plus PN532 companion writer. OpenSpool is useful only as hardware-pattern evidence because its current tag protocol is not Creality CFS-compatible.
