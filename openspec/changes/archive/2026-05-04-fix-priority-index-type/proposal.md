## Why

`PhotoPlugin` registers the `spec.priority` index on `PhotoGroup` as `String.class`, but the underlying field `PhotoGroup.PhotoGroupSpec.priority` is an `Integer`. The index function converts the integer to its decimal string representation (e.g., `10 -> "10"`, `2 -> "2"`). This means the index is sorted lexicographically, so `"10" < "2"`, which contradicts the numeric ordering `10 > 2`. The bug is latent: today the index is only queried by equality, so no visible failure occurs. But if any future code performs a range query or index-backed sort on priority, the results will be silently wrong. Changing the index type to `Integer.class` fixes the ordering at the source with zero knock-on changes.

## What Changes

- Change `IndexSpecs.<PhotoGroup, String>single("spec.priority", String.class)` to `IndexSpecs.<PhotoGroup, Integer>single("spec.priority", Integer.class)` in `PhotoPlugin.java`.
- Update the `indexFunc` to return `Integer` (or `0` for null) instead of converting to `String`.
- No changes to callers, queries, database migrations, or API contracts.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. This is an internal-only index type correction with no spec-level behavior change.

## Impact

- `src/main/java/run/halo/photos/PhotoPlugin.java` — one line type change, one line indexFunc change.
- `photoSortOptions` and other existing specs are unaffected (they sort in-memory, not via the index).
- No migration required; Halo rebuilds extension indexes automatically on scheme re-registration.
