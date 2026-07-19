# Patches pending a real fork

## spoolmansync-direct-mode.patch

Adds direct-Moonraker mode to SpoolmanSync (connect straight to a
printer's Moonraker API instead of going through Home Assistant +
`ha_creality_ws`) — see the patch's own commit message for the full
description, and `spoolmansync/README.md`/upstream for what SpoolmanSync
itself does.

This isn't applied to the `spoolmansync` submodule because the commit it
came from was never actually reachable from the real upstream repository
— it was committed locally in a prior session but this delivery
environment has no permission to create a fork and publish it (GitHub
API repo-creation returns `403 Resource not accessible by integration`).
Pointing the submodule at an unreachable commit would break
`git submodule update --init` for anyone who doesn't already have that
commit cached locally, so the submodule tracks real, verified upstream
`main` instead, and this diff is kept here until it can go somewhere
real.

Regression-tested at the time this patch was generated (`npm install`,
`npx prisma generate`, `npx tsc --noEmit`, `npm run build`, `npx vitest
run` inside `spoolmansync/app` — all clean, 65/65 tests) but never run
against a real printer.

### To apply, once you have push access to your own fork

```bash
gh repo fork gibz104/SpoolmanSync --clone
cd SpoolmanSync
git checkout -b direct-mode
git am ../K2_pro/docs/patches/spoolmansync-direct-mode.patch
git push -u origin direct-mode
```

Then, back in this repo, point the submodule at your fork instead of
upstream and pin it to that branch:

```bash
git config -f .gitmodules submodule.spoolmansync.url <your-fork-url>
git config -f .gitmodules submodule.spoolmansync.branch direct-mode
git submodule sync
cd spoolmansync && git fetch && git checkout direct-mode && cd ..
git add .gitmodules spoolmansync
git commit -m "Point spoolmansync submodule at fork's direct-mode branch"
```
