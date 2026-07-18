# Architecture — writer-focused revision

## Product thesis

OpenFilamentCFS is a tag appliance, not a printer-management platform.

### Primary runtime

`Compose UI -> CFS domain model -> UID-derived codec -> MIFARE Classic writer -> mandatory read-back verification`

### Optional diagnostic runtime

`Expert settings -> direct Creality/Moonraker probe -> normalized compatibility evidence`

The diagnostic path is read-only and has no navigation-level printer dashboard.

## Responsibility allocation

| Responsibility | Authority |
|---|---|
| CFS tag creation and verification | OpenFilamentCFS |
| Tag recognition | CFS firmware |
| CFS slot assignment | SpoolmanSync |
| Print-level filament deduction | SpoolmanSync or Moonraker `[spoolman]` |
| Inventory records | Spoolman |
| Runout switching | Native CFS auto-refill |
| Thermal safeguards | Printer firmware / Klipper |
| Monitoring dashboard and alerts | SpoolmanSync / existing printer UI |

## Why direct printer code remains

The direct client is retained only to collect K2 Pro compatibility evidence and diagnose `box` schema variation. It is not an application subsystem users must configure and it must not grow into a competing dashboard.

## Home Assistant boundary

OpenFilamentCFS does not assume that Home Assistant exists. SpoolmanSync may be deployed in its embedded mode when a user has no existing HA installation. That deployment choice is downstream and separate from the Android app.

## Multi-K2 Pro fleet management

OpenFilamentCFS now manages multiple K2 Pro printers directly. `PrinterRegistry` persists endpoints; `PrinterFleetManager` owns one independent `CrealityDirectClient` per printer and aggregates connectivity, print state, temperatures, CFS slots and alarms. The UI exposes fleet overview and selected-printer controls.

Safe controls use documented Moonraker operations: pause, resume and cancel. CFS transport commands, chamber target changes and arbitrary G-code remain capability-gated until physical K2 Pro validation. Spoolman/SpoolmanSync remain the inventory and deduction authorities.
