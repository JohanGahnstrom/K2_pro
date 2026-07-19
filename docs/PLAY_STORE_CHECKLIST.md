# Play Store Readiness Guide

This replaces the old flat checklist with an ordered guide: what's already
done for you in this repo, the two things that block publishing no matter
what else is checked off, and the exact remaining steps — in order — for
whoever actually publishes this app. Written 2026-07-19, after auditing the
real app source (permissions, `SettingsStore`, `MoonrakerClient`,
`build.gradle.kts`) rather than guessing.

## Hard blockers — resolve these before anything else

Everything else in this document is real, useful work, but **none of it
matters if these two aren't resolved first.** Do not let Play Console setup
momentum carry you past them.

1. **The ported codec's license is unresolved.** `nfc/CfsCodec.kt` and
   `data/MaterialCodes.kt` are a direct port of
   [flamebarke/creality_rfid](https://github.com/flamebarke/creality_rfid),
   which has no LICENSE file upstream (confirmed by cloning it, not
   assumed — see `README.md` § License). This repo's own GPLv3 license
   does not grant you rights over someone else's unlicensed code. Either:
   - contact the upstream author and get explicit permission/license terms
     in writing, or
   - re-derive the codec independently from the underlying AES primitives
     (the algorithm itself — AES-ECB key derivation from a UID — is not
     copyrightable; the specific *implementation* you'd be redistributing
     is what's in question).

   Do not submit to the Play Store until one of these is done.

2. **Nothing has been verified on real hardware.** Device compatibility
   (H-000), the actual tag write (H-001), the blank-tag sector-trailer path
   (H-011), and the K2 Pro `box`/`M8200` behavior (H-004) are all built
   from community documentation and cryptographic proof — never bench
   tested. See `docs/HOLD_REGISTER.md` for the full list. This isn't
   optional polish: publishing tag-writing to real users before this is
   done risks writing bad data to their tags with no way for the app to
   know it's wrong. At minimum, bench-verify H-000/H-001/H-011 yourself
   before any public release, even a closed test.

## Done for you in this repo

These are real, verifiable changes — check `docs/BUILD_STATUS.md` for the
CI run that confirmed each one compiles and passes lint:

- **Adaptive launcher icon.** The manifest previously pointed `android:icon`
  directly at a raw vector drawable (no background/foreground separation,
  no themed-icon support). Replaced with a proper adaptive icon
  (`res/mipmap-anydpi-v26/ic_launcher.xml` + background/foreground/
  monochrome layers), using the app's real brand colors.
- **Release signing wired up, safely.** `app/build.gradle.kts` now reads
  `RELEASE_STORE_FILE`/`RELEASE_STORE_PASSWORD`/`RELEASE_KEY_ALIAS`/
  `RELEASE_KEY_PASSWORD` from a git-ignored `local.properties` (or
  environment variables), and only signs the release build type if all
  four are present. Until you add a real keystore, behavior is unchanged
  — `bundleRelease` still produces the same unsigned AAB it always did.
- **Real Play Store graphics**, rendered from the app's actual brand
  colors (not a template): `docs/play_store/assets/icon-512.png` (512×512)
  and `docs/play_store/assets/feature-graphic-1024x500.png` (1024×500).
- **Draft privacy policy**: `docs/play_store/PRIVACY_POLICY.md`, written
  directly from an audit of what the app actually stores and sends over
  the network (nothing leaves the device except requests to a printer URL
  *you* type in).
- **Draft Data Safety form answers**: `docs/play_store/DATA_SAFETY.md`,
  mapped category-by-category to the app's real behavior.
- **Draft store listing copy**: `docs/play_store/STORE_LISTING.md` — short
  and full descriptions, content rating guidance, target audience, the
  required NFC/MIFARE-variance disclosure, and a first-release "what's
  new" blurb.

## What's left — in order

### 1. Generate a real release keystore

Never commit this. `.gitignore` already excludes `*.jks`/`*.keystore`/
`local.properties`.

```bash
keytool -genkeypair -v -keystore release.jks -keyalg RSA -keysize 2048 \
  -validity 10000 -alias openfilamentcfs
```

Add to `local.properties` (already git-ignored):

```properties
RELEASE_STORE_FILE=/absolute/path/to/release.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=openfilamentcfs
RELEASE_KEY_PASSWORD=...
```

Then `./gradlew bundleRelease` produces a real signed AAB.

### 2. Create a Play Console developer account

One-time $25 fee, at [play.google.com/console](https://play.google.com/console).

### 3. Create the app entry

- Application ID `com.openfilament.cfs` is already set in
  `app/build.gradle.kts` — confirm it's not already taken (it's
  project-specific enough that a collision is unlikely, but Play Console
  will tell you immediately if it is).
- Enable **Google Play App Signing** when prompted on first upload — Google
  then manages the actual signing key; your `release.jks` becomes the
  upload key only.

### 4. Fill in Data Safety and content rating

Use `docs/play_store/DATA_SAFETY.md` and the content-rating section of
`docs/play_store/STORE_LISTING.md` as drafts — read the caveats in each,
since Play's exact form wording changes over time.

### 5. Host the privacy policy publicly

`docs/play_store/PRIVACY_POLICY.md` needs a real public URL, not just a
file in the repo. Simplest path: enable GitHub Pages for this repo
(Settings → Pages → deploy from a branch) and link directly to it, or copy
its contents anywhere else you control. Fill in the effective date and a
real contact first.

### 6. Upload store listing content

Short/full description, content rating answers, and the two rendered
graphics all come from `docs/play_store/STORE_LISTING.md` and
`docs/play_store/assets/`.

### 7. Get real screenshots

**Not producible in this environment** — no Android emulator is available
here (no `/dev/kvm`, no CPU virtualization extensions passed through,
confirmed by trying). You'll need a real device or an emulator on your own
machine. Play requires at minimum 2 phone screenshots (up to 8), 16:9 or
9:16. Capture Home, Tag (both Simple and Expert mode), and Spools for a
representative set.

### 8. Closed testing

Run a closed testing track before anything goes to production — ideally
with real K2 Pro/CFS owners, given the hard blockers above. Use their
feedback to close H-000/H-001/H-011/H-004 for real, not just in the
checklist.

### 9. Review the pre-launch report

Play Console runs your APK/AAB on a range of real devices automatically
and reports crashes/ANRs — review this before promoting past closed
testing.

### 10. Publish source and third-party notices

GPLv3 requires source availability — this public GitHub repo already
satisfies that. Link to it (and to `THIRD_PARTY.yml`) from the store
listing or an in-app "About" link if you want to be extra clear (Settings
already shows a short attribution note).

### 11. Promote to production

Only once: closed testing is clean, the pre-launch report has nothing
alarming, and — non-negotiably — the two hard blockers at the top of this
document are actually resolved, not just checklisted.
