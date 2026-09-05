# ADR 0002 — Minimal deviations from the specified Room schema

## Status
Accepted (F0)

## Context
The spec says "implement exactly this schema", but Room has hard requirements
the spec text cannot satisfy as written.

## Decision
Keep every table, column, index and bitmask exactly as specified, with three
minimal, purely additive/technical deviations:

1. **WearLog** gets a surrogate `@PrimaryKey(autoGenerate=true) id: Long = 0`
   plus indices on `date` and `garmentId` (stats queries). The logical key
   remains `(date, outfitId, garmentId)`; export/import (F1) must treat
   wear_log as an append-only log keyed on that triple, never on `id`.
2. **Tag** PK becomes `autoGenerate = true` with default `0` (spec showed a
   bare Long PK; without autoGenerate every insert needs a hand-rolled id).
3. **Loan** declares `@PrimaryKey` on `garmentId` — one loan row per garment,
   matching the relational rule "dynamic state lives in relations".

## Consequences
- Schema v1 golden (app/schemas) includes the deviations from day one, so
  migrations never need to reconcile them.
- data.json (F1) serializes WearLog without the surrogate id.
- Adding DAOs for trips/outfits/tags in F2/F3 does NOT change the schema
  (no migration needed); adding tables or columns does.
