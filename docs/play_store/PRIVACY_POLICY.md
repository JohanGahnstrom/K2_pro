# Privacy Policy — OpenFilament CFS

**Effective date:** [fill in before publishing]
**Contact:** [fill in a support email/contact you control — see note at the bottom]

This is a draft written directly from the app's actual source code (not a
template), so every claim below is checked against real behavior at the time
of writing. Confirm it still matches before publishing, and re-check it
whenever data handling changes.

## Summary

OpenFilament CFS does not have user accounts, does not use analytics or
advertising, and does not send your data to the developer or to any
third-party server. Everything the app stores, it stores on your device.
The only network requests it makes go to a printer address or dashboard URL
that **you** type into the app's Settings/Printer screen — never to an
address chosen by the developer.

## What the app accesses on your device

| Permission | Why | Requested at |
|---|---|---|
| NFC | To read and write the CFS RFID tag data — this is the app's core function | Install time (not a runtime prompt on most devices) |
| Internet | To talk to the Moonraker API on your printer, and to open a SpoolmanSync URL you provide | Install time |
| Access network state | To check basic connectivity before attempting a printer connection | Install time |

The app requests no other permissions — no camera, location, contacts,
storage, microphone, or background access.

## Data the app stores locally

The app stores the following on-device only, via Android's DataStore
(private app storage, not accessible to other apps):

- Your printer's Moonraker URL and, if entered, a SpoolmanSync URL
- Whether you're in Simple or Expert mode
- A local history of spools you've tagged: product, colour, weight, the
  serial number written to the tag, and the date/time — used only to show
  you your own tagging history inside the app
- Whether you've dismissed the first-run guide prompt

None of this is transmitted anywhere. Uninstalling the app removes it,
consistent with standard Android app-data behavior.

## Data the app reads and writes via NFC

When you write a tag, the app encodes filament identity data (material,
colour, weight, a generated serial number, and today's date) onto a MIFARE
Classic 1K tag using the published Creality CFS encryption scheme. This data
is written to the physical tag and stored in the local history above — it
is never sent over the network.

## Network requests the app makes

The app makes outbound HTTP requests **only** to the printer URL you enter
yourself (Settings → Printer), to:

- Query printer/CFS status (`/printer/objects/query`)
- Toggle the native auto-refill feature and send load/unload/chamber
  commands (`/printer/gcode/script`)

These requests go to your own printer, on your own network, at an address
you control — not to any server operated by the developer. The app also
lets you open a SpoolmanSync URL you provide in your device's browser; this
is just launching a link, not sending app data to it.

**The developer operates no backend server for this app.** There is nothing
for the developer to receive, log, or store, because nothing is sent to
them.

## Third parties

The app does not integrate any third-party analytics, advertising, or
crash-reporting SDK. See [`THIRD_PARTY.yml`](../../THIRD_PARTY.yml) for the
open-source code this project is built on or references — none of it
introduces data collection.

## Children's privacy

This app is a hobbyist/maker tool for configuring 3D-printer filament
spools. It is not directed at children and does not knowingly collect any
information from anyone.

## Changes to this policy

If data handling ever changes, this file will be updated and the effective
date above will be revised.

## Contact

Questions about this policy: **[fill in a real, monitored contact — an
email address or a link to this repository's issue tracker]**.

---

### Note for whoever publishes this

Play Console requires a **public, reachable URL** for this policy, not a
file in a git repo. The simplest option: enable GitHub Pages for this repo
(Settings → Pages → deploy from a branch) and link directly to this file,
or copy its contents into a plain page hosted anywhere you control. Fill in
the effective date and a real contact before publishing either way.
