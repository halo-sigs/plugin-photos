## Context

The plugin originally treated photos without `exif.dateTimeOriginal` as a separate tail group in shooting-time sort. The `2026-05-02-combine-photo-sort-time` change unified that behavior so shooting-time sort uses an "effective time" — `exif.dateTimeOriginal` when present, `metadata.creationTimestamp` otherwise. That semantic was implemented in two layers:

1. **API contract** — `PhotoQuery.getSort()` and `PhotoPublicQuery.getSort()` emit `Sort.by(desc("exif.dateTimeOriginal"), desc("metadata.creationTimestamp"), asc("metadata.name"))` and document `sort=exif.dateTimeOriginal,*` as effective-time semantics.
2. **Implementation** — `PhotoServiceImpl.listPhotoByEffectiveTime()` and `PhotoPublicQueryServiceImpl.listPhotosByEffectiveTime()` short-circuit the normal paged query, calling `client.listAll(...)` and applying `PhotoSortUtils.effectiveTimeComparator` in memory because the database has no field that holds the effective time.

The in-memory branch is the source of the P1: every effective-time-sorted request loads the full table, sorts in memory, then sublists for pagination. After `2026-05-04-fix-page-size-limit-1000` introduced `PhotoPublicQueryServiceImpl#listAllPhotos` (paged iteration of size 500 via `expand`), the effective-time short-circuit makes each page invocation re-execute `client.listAll(...)`, so iterating a 50 000-photo gallery becomes O(N²) work and proportionally O(N) memory per page fetch.

`PhotoPlugin#start` already registers a computed index for `exif.dateTimeOriginal` using `IndexSpecs.<Photo, String>single(...).indexFunc(photo -> dt.toString())`. The same mechanism can store the effective time, eliminating the in-memory short-circuit.

```
Before                                   After
----------------------------------       ----------------------------------
listPhotos(query)                         listPhotos(query)
  └── isEffectiveTimeSort? ──┐                 └── client.listBy(
       ├── yes                │                       options,
       │   └── client.listAll        ⇒                PageRequest(
       │       (full table)                              sort=effectiveTime
       │       .sort(effComparator)                      ))
       │       .sublist(page,size)                       (DB-served, page-bounded)
       └── no
           └── client.listBy(page)
```

The fix is purely an implementation change: API contract, sort-parameter values, sort order, response shapes — all unchanged.

## Goals / Non-Goals

**Goals:**
- Eliminate the per-request full-table load for effective-time sort in both the console (`PhotoServiceImpl`) and the public read-path (`PhotoPublicQueryServiceImpl`).
- Keep request memory and CPU bounded by `pageSize`, not by total gallery size, for every effective-time-sorted request.
- Preserve observable behavior: same sort order, same default ordering, same `sort` query-parameter values.
- Preserve existing computed-index conventions in `PhotoPlugin` (computed `IndexSpec` returning `dt.toString()`, empty string for null).

**Non-Goals:**
- Adding a real persisted field on `Photo.spec` (no schema change, no migration).
- Changing the public `sort` parameter name. Clients continue to send `sort=exif.dateTimeOriginal,(asc|desc)`.
- Reworking `PhotoSortUtils.effectiveTimeComparator`. It is retained as a reference and remains useful for unit tests.
- Changing pagination defaults, group/tag filtering, or `PhotoFinder`/`PhotoRouter` SSR behavior.

## Decisions

### 1. Add a new `effectiveTime` IndexSpec rather than modifying the existing `exif.dateTimeOriginal` index

A computed `IndexSpec` is registered on `Photo` that stores the effective time per row:

```java
indexSpecs.add(IndexSpecs.<Photo, String>single("effectiveTime", String.class)
    .indexFunc(photo -> {
        Instant dt = photo.getExif() == null ? null : photo.getExif().getDateTimeOriginal();
        if (dt != null) {
            return dt.toString();
        }
        Instant created = photo.getMetadata() == null
            ? null : photo.getMetadata().getCreationTimestamp();
        return created == null ? "" : created.toString();
    }));
```

- **Rationale**:
  - Keeps the existing `exif.dateTimeOriginal` index semantically pure (only EXIF time), preserving its usefulness for any future feature that needs strict EXIF filtering or sorting.
  - The new index name documents intent: "this is the effective time, with fallback baked in."
  - The name is intentionally **top-level** (`effectiveTime`, not `spec.effectiveTime`). Other Photo indexes follow a `spec.*` / `exif.*` convention because each one mirrors a real field path on `Photo` (`spec.groupName`, `spec.displayName`, `spec.priority`, `exif.make`, etc.). `effectiveTime` is a derived value with no corresponding field on `PhotoSpec`, so prefixing it with `spec.` would falsely imply that such a field exists. A flat name signals "this is an index-only derivation".
  - ISO-8601 string lexicographic order matches `Instant` natural order, so the computed `String` index can be sorted directly by the storage layer with no conversion.
  - Mirrors the convention already used by `PhotoPlugin` for `exif.dateTimeOriginal` (compute in `indexFunc`, store as `String`, default `""` for null).

- **Alternative considered**: Modify the existing `exif.dateTimeOriginal` IndexSpec's `indexFunc` to include the fallback. Rejected because the index name would no longer match its content (it would actually store effective time, not EXIF time), and any future caller filtering on "photos that have EXIF time" would silently get false positives.

### 2. Translate the sort key inside `PhotoQuery` / `PhotoPublicQuery`

`PhotoQuery.getSort()` and `PhotoPublicQuery.getSort()` are the only authoritative builders of the `Sort` passed to `PageRequestImpl`. Both currently emit `desc("exif.dateTimeOriginal")` as the primary key. After this change they emit `desc("effectiveTime")` for the same logical sort; the public API parameter name `exif.dateTimeOriginal` is mapped to `effectiveTime` at this boundary.

```java
@Override
public Sort getSort() {
    var sort = resolvedSort();
    if (sort.isUnsorted()) {
        return Sort.by(
            Sort.Order.desc("effectiveTime"),
            Sort.Order.desc("metadata.creationTimestamp"),
            Sort.Order.asc("metadata.name")
        );
    }
    // Map the documented public param name to the internal index name.
    var translated = Sort.by(sort.stream()
        .map(o -> DATE_TIME_ORIGINAL_SORT.equals(o.getProperty())
            ? new Sort.Order(o.getDirection(), "effectiveTime")
            : o)
        .toList());
    return translated.and(Sort.by(
        Sort.Order.desc("metadata.creationTimestamp"),
        Sort.Order.asc("metadata.name")
    ));
}
```

- **Rationale**: Centralises the public-name → internal-index mapping in the same class that already documents the sort grammar. Endpoint/finder/router callers stay unaware of the index name. Future renames stay confined to these two classes.
- **Alternative considered**: Push the translation into `PhotoServiceImpl` / `PhotoPublicQueryServiceImpl`. Rejected because both services would need duplicate translation logic, and the natural locus of "what does this sort token mean" is the query class.

### 3. Remove the in-memory effective-time short-circuit from both services

`PhotoServiceImpl.listPhoto` and `PhotoPublicQueryServiceImpl.listPhotos` collapse to a single path:

```java
return client.listBy(Photo.class, options,
    PageRequestImpl.of(page, size, sort));   // sort already routed through getSort()
```

`isEffectiveTimeSort()` and `isEffectiveTimeAscending()` on `PhotoQuery` / `PhotoPublicQuery` become unused after this collapse. Drop them along with the in-memory branch and the `Sort.unsorted()` `client.listAll(...)` call.

- **Rationale**: The reason these helpers and branches existed was the index gap; once the gap is closed, they are dead code.
- **Alternative considered**: Keep `isEffectiveTimeSort()` for callers outside the service. None exist today (verified by usage scan), so retaining them adds dead surface.

### 4. Retain `PhotoSortUtils.effectiveTimeComparator` for tests

The comparator is no longer used in the request hot path. Keep it for unit tests that pin the desired ordering semantics — particularly the tie-break on `metadata.creationTimestamp desc` and `metadata.name asc`. New unit tests SHOULD assert that `client.listBy(... Sort.by(desc("effectiveTime"), desc("metadata.creationTimestamp"), asc("metadata.name")))` produces the same ordering as the comparator over a fixture set.

- **Rationale**: Two independent expressions of the same ordering rule provide a regression net.

## Risks / Trade-offs

- **[Risk]** Composite sort across mixed indexed/system fields (`effectiveTime` + `metadata.creationTimestamp` + `metadata.name`).
  → **Mitigation**: `metadata.creationTimestamp` and `metadata.name` are standard system fields that Halo's index layer already supports for sort; existing code (`PhotoFinderImpl.defaultPhotoSort()`, the `2026-05-04-fix-page-size-limit-1000` paged-iteration code) already chains them with `exif.dateTimeOriginal` without issue. The new key behaves the same way.

- **[Risk]** Edge case: a `Photo` with both EXIF time and creation timestamp absent indexes as `""`. Empty string sorts first in ascending order but last in descending order, which differs from the previous in-memory comparator's `Comparator.nullsLast(...)` (nulls last regardless of direction).
  → **Mitigation**: In practice every Halo extension is created with `metadata.creationTimestamp` populated; this edge case requires a malformed extension. Document the behavior; do not encode a sentinel value (e.g., far-past or far-future date) because that hides data quality issues instead of surfacing them.

- **[Risk]** Plugin restart recomputes the new index for every existing photo.
  → **Mitigation**: This is the standard behavior for any new `IndexSpec` and is not specific to this change. Index population is per-photo and bounded by gallery size; large galleries pay this cost once at startup, not per request.

- **[Trade-off]** Two separate indexes (`exif.dateTimeOriginal` and `effectiveTime`) increase the index footprint by one column-equivalent per photo. Acceptable: keeping the EXIF index pure preserves a useful filter that no other index covers.

## Migration Plan

No data migration needed. Steps on first plugin restart after deployment:

1. Halo invokes `PhotoPlugin#start`, which registers the new `effectiveTime` IndexSpec.
2. Halo's index layer recomputes the column for every existing `Photo` by calling the `indexFunc`.
3. New / updated photos populate the index automatically on each save.

No backfill job, no schema migration, no manual operator action.

## Open Questions

- **Tests for empty-string ordering**: Should the unit test fixture include a photo with no EXIF time and no `metadata.creationTimestamp` to lock in the asc-first/desc-last behavior? Probably yes for documentation value, even though the case is theoretical. Decision deferred to test phase.
