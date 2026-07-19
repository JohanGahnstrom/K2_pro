# Play Store listing — draft copy

Draft text for the Play Console store listing fields. Written to be
honest about the app's actual maturity (see `docs/HOLD_REGISTER.md` — real
hardware is not yet bench-verified) rather than oversell it; adjust the
"early access" framing once that changes.

## Short description (max 80 characters)

```
Write real CFS RFID tags for third-party filament spools on your Creality K2.
```
(77 characters)

## Full description (max 4000 characters)

```
OpenFilament CFS lets you use any third-party filament spool with your
Creality K2 Pro's CFS (Cargo Feeding System) — no Creality-branded spool
required.

Creality's CFS identifies filament through an encrypted RFID tag. Official
Creality spools carry one; everyone else's don't, so third-party filament
shows up unlabelled on the printer. This app closes that gap: pick your
filament, confirm colour and spool size, then hold a blank MIFARE Classic
1K tag against your phone to write and verify a real, CFS-compatible tag —
built from the same cryptographic scheme the community has already
reverse-engineered and published, not a guess.

WHAT IT DOES
• Simple Mode: three choices — filament, colour, spool size — no protocol
  detail in sight.
• Expert Mode: full mapping visibility, technical field values, and
  capability-gated printer tools for anyone who wants to see exactly what's
  being written.
• Every write is read back and verified — the app never reports success
  without confirming the tag actually took the data.
• Basic printer status from Moonraker: online/offline, temperatures, print
  progress, and a native auto-refill toggle.
• In-app step-by-step guides: how to scan and write a tag, how to
  physically apply a tag to a spool, and how to safely reuse or move one.
• A local, on-device history of every spool you've tagged.

WHAT IT DELIBERATELY DOESN'T DO
This app is a thin client, not a full CFS management suite. Slot
dashboards, per-print consumption tracking, and low-stock alerts are
better handled by SpoolmanSync, a separate open-source project this app
is designed to work alongside — see the project's GitHub for details.

BEFORE YOU INSTALL
• Requires a phone with NFC hardware capable of MIFARE Classic 1K — this
  varies by manufacturer and chipset, and the app checks and tells you
  plainly if your phone can't do it, before you try to write anything.
• Requires blank (or reusable) MIFARE Classic 1K tags, sold separately —
  not the NTAG213/215/216 stickers most generic "NFC tag" listings sell.
• This is an early-access release. The core codec is cryptographically
  verified against a published reference, and the app passes its full
  automated test suite on every release — but it has not yet been
  bench-tested against a physical K2 Pro by the developer. Please report
  anything that doesn't match your hardware.

OPEN SOURCE AND INDEPENDENT
OpenFilament CFS is fully open source (GPLv3) and not affiliated with or
endorsed by Creality. No account, no ads, no analytics — see the in-app
Settings screen and this app's privacy policy for specifics. Source code,
issue tracker, and full technical documentation:
https://github.com/JohanGahnstrom/K2_pro
```

## Content rating questionnaire — expected answers

This is a straightforward utility app; the questionnaire should come back
clean:

| Question area | Answer |
|---|---|
| Violence | None |
| Sexual content | None |
| Profanity | None |
| Controlled substances | None |
| Gambling | None |
| User-generated content shared with others | None — spool history is local-only, never shared or uploaded |
| User-to-user communication | None |
| Location sharing | None |
| Personal information sharing | None |
| Digital purchases | None |

Expected outcome: the lowest available rating tier (e.g. "Everyone" /
"PEGI 3" equivalent, depending on the rating bodies Play Console applies
in your region).

## Target audience and content

- **Not designed for children.** This is a 3D-printing hobbyist/maker
  tool; select the general/adult target audience options, not the
  children's-content flow.
- No ads, so the "contains ads" declaration is **No**.
- No in-app purchases, so **No** there too.

## NFC / MIFARE Classic disclosure

Google Play increasingly expects hardware-dependent apps to be upfront
about variance. Suggested store-listing language (already folded into the
full description above, repeated here in case you want it as a standalone
line):

> Requires a phone with NFC hardware that supports MIFARE Classic 1K.
> This varies between manufacturers and even between models from the same
> manufacturer — the app checks your specific phone and tells you plainly
> if it isn't supported, before you try to write a tag.

## "What's new" — first release

```
First public release.

• Write and verify real CFS-compatible RFID tags for third-party filament
  spools, using a cryptographically verified codec.
• Simple and Expert modes.
• In-app guides for scanning, writing, and physically applying tags.
• Basic Moonraker printer status and native auto-refill toggle.
• Local, on-device spool tagging history.

This is an early-access release — see the project's GitHub for current
known limitations and to report issues.
```

## Graphics

- **Icon** (512×512): [`assets/icon-512.png`](assets/icon-512.png) —
  rendered from the app's actual in-app launcher icon colors and mark.
- **Feature graphic** (1024×500): [`assets/feature-graphic-1024x500.png`](assets/feature-graphic-1024x500.png).
- **Phone/tablet screenshots**: not producible in this environment (no
  Android emulator or physical device available — see
  `docs/PLAY_STORE_CHECKLIST.md`). These need to come from a real device
  running the app.
