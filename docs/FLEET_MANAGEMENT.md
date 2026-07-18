# Fleet Management

The app supports any number of locally reachable K2 Pro printers.

- Persistent printer name, host, location and ports
- Independent WebSocket/Moonraker fallback connection per printer
- Fleet counts: configured, online, printing and alarmed
- Per-printer progress, file, temperatures and CFS slot status
- Pause, resume and cancel through Moonraker
- Capability discovery for CFS object, chamber telemetry and camera kind
- No dependency on Home Assistant

Not yet enabled pending physical validation: CFS load/unload, auto-refill mutation, heater target mutation and camera rendering.
