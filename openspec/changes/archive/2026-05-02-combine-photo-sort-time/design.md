## Context

The current photo sorting contract already exposes shooting-time and creation-time sort options through the console API and UI. Shooting-time sort is represented as `spec.dateTimeOriginal,asc|desc`, with `metadata.creationTimestamp` and `metadata.name` used as secondary tie-breakers. This works for photos with EXIF time, but photos without EXIF time are ordered after the EXIF-present set instead of being placed by their creation timestamp in the same timeline.

The desired behavior is an effective photo time sort key:

1. Use `spec.dateTimeOriginal` when available.
2. Fall back to `metadata.creationTimestamp` when EXIF time is missing.
3. Use deterministic tie-breakers after the effective time.

Spring Data `Sort` cannot directly express this coalescing rule over two fields, so the implementation needs application-level comparator logic wherever shooting-time semantics are required.

## Goals / Non-Goals

**Goals:**

- Make default console photo ordering use effective photo time descending.
- Make explicit `sort=spec.dateTimeOriginal,asc|desc` use effective photo time in the requested direction.
- Make theme finder default ordering use the same effective photo time descending rule.
- Keep `sort=metadata.creationTimestamp,asc|desc` as a pure creation-time sort.
- Keep existing sort option labels and query values stable.

**Non-Goals:**

- Add new frontend sorting controls.
- Change EXIF extraction or upload behavior.
- Add a stored denormalized sort field or migrate existing Photo resources.
- Change group filtering, tag filtering, keyword filtering, or cascade delete behavior.

## Decisions

### Decision 1: Use a shared comparator for effective photo time

Use a backend comparator that derives an `Instant` from each photo by reading `spec.dateTimeOriginal` first and `metadata.creationTimestamp` second. The comparator should support ascending and descending directions and append `metadata.creationTimestamp` plus `metadata.name` tie-breakers to keep order stable.

Alternative considered: add a persisted `spec.effectiveTime` field. That would allow normal indexed field sorting but requires migration/backfill and ongoing write-time synchronization whenever EXIF data changes. The current change only needs behavioral correction and does not require schema churn.

### Decision 2: Keep API values unchanged

Continue accepting `sort=spec.dateTimeOriginal,asc|desc` and reinterpret it as effective photo time sorting. This preserves frontend compatibility and keeps user-facing labels unchanged. `sort=metadata.creationTimestamp,asc|desc` remains unchanged for users who explicitly want upload/create order.

Alternative considered: introduce a new `sort=effectiveTime,desc` option. That would make the behavior more explicit, but it would require OpenAPI and frontend changes while leaving the existing problematic option in place.

### Decision 3: Apply comparator only after existing filtering

The console list should continue using existing query filters for keyword, group, ungrouped, and tag. After the filtered photo stream/list is produced, apply the effective-time comparator for shooting-time sorts before pagination is returned. Creation-time sorts can continue to use the existing field sort path if it already produces the correct behavior.

Alternative considered: always sort all modes in memory. This simplifies code but may unnecessarily bypass existing storage-level ordering for pure creation-time sorts.

### Decision 4: Align finder default ordering with console semantics

The finder default sort should use the same effective-time descending comparator so theme routes and template calls do not drift from the console's default view. Existing group filtering in `listBy` and grouping behavior in `groupBy` should remain unchanged apart from per-photo ordering.

## Risks / Trade-offs

- Application-level sorting may require loading filtered photos before pagination -> keep this limited to effective-time sorting where field `Sort` cannot express the fallback.
- Existing users may notice photos without EXIF moving upward in shooting-time views -> this is the intended behavior and matches the corrected contract.
- If both EXIF time and creation timestamp are missing, comparator behavior needs a null-safe fallback -> use null-safe comparison and keep `metadata.name` as the final deterministic tie-breaker.
- Large galleries could pay a higher in-memory sorting cost for shooting-time sorts -> acceptable for the current plugin model, and creation-time sorts can stay storage-backed.

## Migration Plan

No data migration is required. Deploying the change updates read-time ordering only. Rollback is a code rollback to the previous field-sort behavior.

## Open Questions

None.
