# Play Console Data Safety form — draft answers

**This is not legal advice, and Play's Data Safety questionnaire wording
changes over time.** These are proposed answers derived directly from an
audit of this app's actual code (permissions declared, what
`SettingsStore`/DataStore persists, what `MoonrakerClient` sends over the
network) — confirm each answer against the live Play Console form and
current Google Play policy before submitting.

## The one judgment call to make before submitting

Play's Data Safety form asks about data the app **collects or shares** —
generally meaning data transmitted off the device to the developer or to a
third party the developer integrates. This app:

- Stores everything (printer URL, mode, spool history) locally on-device
  via DataStore — never transmitted anywhere.
- Sends network requests only to a Moonraker printer URL **the user types
  in themselves** — the developer operates no backend and receives nothing.

Under a plain reading of "collect," none of this is data collection by the
developer, so the recommended declaration is **"No data collected."**
That's the position these draft answers take. If you'd rather be
conservative given the app *does* handle spool/product data (even though
only locally), see the alternate wording in the last section.

## Recommended form answers

**Does your app collect or share any of the required user data types?**
→ **No**

If the form requires walking through each category anyway, here's the
per-category answer and why:

| Category | Collected? | Why |
|---|---|---|
| Location | No | App requests no location permission and never reads location |
| Personal info (name, email, address, etc.) | No | No account system, no forms asking for personal info |
| Financial info | No | No payment, billing, or transaction handling anywhere |
| Health and fitness | No | Not applicable |
| Messages | No | Not applicable |
| Photos and videos | No | No camera or media-picker access |
| Audio files | No | Not applicable |
| Files and documents | No | No document access beyond the app's own private DataStore |
| Calendar | No | Not applicable |
| Contacts | No | Not applicable |
| App activity | No | No analytics, no in-app search history, no install tracking |
| Web browsing | No | The SpoolmanSync URL is opened via the device's own browser (`Intent.ACTION_VIEW`), not tracked or logged by this app |
| App info and performance | No | No crash reporting or performance-monitoring SDK is integrated |
| Device or other IDs | No | No advertising ID, no device fingerprinting |

**Is all user data encrypted in transit?**
→ Network requests to your printer use whatever scheme your Moonraker URL
specifies (typically plain HTTP on a local network — `usesCleartextTraffic`
is enabled in the manifest for exactly this reason). There's no
developer-operated server to encrypt traffic to.

**Do you provide a way for users to request data deletion?**
→ All data is local; uninstalling the app deletes it. Within the app, the
Spools screen also lets a user delete individual history entries directly.

**Privacy policy URL**
→ Point to a published version of
[`PRIVACY_POLICY.md`](PRIVACY_POLICY.md) — see that file's own note on
hosting it (e.g., GitHub Pages).

## If you'd rather be conservative

If you'd prefer to err on the side of over-declaring rather than rely on
the "stays on-device = not collected" reading above, the only category
worth reconsidering is **App activity**, specifically "App interactions" —
since the app does locally record a history of what you've tagged. Even in
that conservative reading, the honest declaration would still be:
processed, **not shared with any third party, and not transmitted off the
device** — Play's form has fields for exactly this distinction ("data is
processed ephemerally" / "data isn't transmitted off the device" toggles
in newer versions of the form).
