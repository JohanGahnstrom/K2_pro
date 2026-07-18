# Hold register

## H-000 — representative Android MIFARE Classic coverage

**Status:** open, highest risk.

Validate the hard gate on a representative device matrix. If incompatibility exceeds the product threshold, implement the ESP32+PN532 companion path rather than weakening verification.

## H-001 — CFS codec and key derivation

**Status:** closed.

Implemented from established community sources with a golden-vector regression test.

## H-002 — K2 Pro direct object schema

**Status:** diagnostic only.

The app retains a read-only probe to capture K2 Pro differences. This does not block tag writing.

## H-003 — blank-tag sector trailer variants

**Status:** hardware validation required.

Test multiple MIFARE Classic 1K sticker suppliers and preserve access bytes during conversion.

## H-004 — firmware compatibility

**Status:** ongoing validation.

Re-test written tags following K2/CFS firmware updates.

## H-005 — slot dashboard

**Status:** removed from scope.

Use SpoolmanSync rather than implementing another persistent CFS tray dashboard.

## H-006 — direct camera

**Status:** removed from scope.

Use the stock printer UI or existing monitoring stack.

## H-007 — consumption accounting and alerts

**Status:** removed from scope.

Use SpoolmanSync or Moonraker `[spoolman]` plus Spoolman.

## H-008 — serial-to-spool identity convention

**Status:** retained as integration convention.

Prefer a six-digit serial aligned with the Spoolman spool ID where deployment constraints allow it. Validate end-to-end with CFS and SpoolmanSync.

## H-009 — ESP32+PN532 fallback

**Status:** conditional.

Implement only if phone compatibility evidence warrants it. OpenSpool and DnG/soylentOrange hardware patterns are references; OpenSpool's NDEF codec is not reusable for CFS.
