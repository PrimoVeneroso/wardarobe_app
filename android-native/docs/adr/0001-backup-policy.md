# ADR 0001 — Backup policy: nothing leaves the device uninvited

## Status
Accepted (F0)

## Context
Armadio's contractual promise is privacy-first, 100% offline, zero permissions.
The database and the wardrobe photos are the most sensitive data in the app.
Android Auto Backup / device transfer would upload an unencrypted copy of both
to Google's cloud (or hand it to a transfer tool), silently breaking that promise.

## Options considered
1. `allowBackup="true"` + exclusion rules for database and images.
2. `allowBackup="false"` + defensive exclusion rules.

## Decision
Option 2. `allowBackup="false"` disables OS-managed backup and transfer entirely.
`dataExtractionRules` (API 31+) and `fullBackupContent` (API < 31) are declared
anyway, excluding database, `filesDir/wardrobe`, DataStore and shared prefs:
defense in depth against a future accidental flip of `allowBackup` or OEM
tooling that ignores the flag.

## Consequences
- No automatic restore on reinstall or device switch — by design.
- The ONLY supported backup path is the user-driven export ZIP (F1).
- Onboarding must warn: "uninstalling deletes your data — export first" (F1).
- Lint warnings about rules declared alongside `allowBackup=false` are accepted
  and documented here.
