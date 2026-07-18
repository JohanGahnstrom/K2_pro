# Downstream deployment options

OpenFilamentCFS has no Home Assistant runtime dependency.

## Option A — SpoolmanSync

Best complete downstream experience:

- CFS tray/spool assignment;
- automatic print deductions;
- low-stock alerts;
- QR/NFC matching;
- multi-printer dashboard.

SpoolmanSync can connect to an existing Home Assistant deployment or run its own embedded HA environment. That embedded runtime belongs to the server deployment, not to this Android application.

## Option B — native Moonraker plus Spoolman

Best minimal no-HA deployment:

- configure Moonraker's `[spoolman]` component;
- set/clear the active spool through Moonraker/Klipper;
- let Moonraker report print consumption to Spoolman.

This option does not provide SpoolmanSync's CFS tray dashboard and alerts, but avoids any Home Assistant runtime.

## Option C — tag writing only

The CFS can use the written identity without any inventory server. Native CFS auto-refill remains a printer function.
