## Why

Effective-time sort (`exif.dateTimeOriginal` falling back to `metadata.creationTimestamp`) is the default order for both the console photo list and the public `GET /photos` endpoint. The current implementation cannot push that fallback into the database layer, so both `PhotoServiceImpl.listPhotoByEffectiveTime()` and `PhotoPublicQueryServiceImpl.listPhotosByEffectiveTime()` use `client.listAll(...)` to load every matching `Photo` into memory, sort with `PhotoSortUtils.effectiveTimeComparator`, then sublist for pagination.

For galleries at scale (tens of thousands of photos) this:

- Allocates the full result set on the heap on every effective-time-sorted request.
- Generates GC churn in proportion to gallery size, not to page size.
- Adds latency that scales linearly with gallery size, including for the small page the caller actually wants.
- Compounds with the paged-iteration fix in `2026-05-04-fix-page-size-limit-1000`: `PhotoFinder.listAll()` and `PhotoRouter.loadFilteredPhotos()` now call `listAllPhotos`, which iterates pages of 500 — but every page hits `listPhotosByEffectiveTime`, so each page re-loads the full table, turning O(N) work into O(N²).

The semantic gap is fixable: ISO-8601 `Instant.toString()` lexicographic order matches `Instant` natural order, and Halo's `IndexSpecs` already supports computed indexes via `indexFunc` (see existing `exif.dateTimeOriginal` index in `PhotoPlugin#start`). A computed index that stores the effective time per photo lets the database serve effective-time sort with the same cost profile as any other indexed sort.

## What Changes

- Add a computed `IndexSpec` named `effectiveTime` on `Photo` whose `indexFunc` returns `(exif.dateTimeOriginal ?? metadata.creationTimestamp).toString()` (empty string when both are absent). The name is intentionally top-level (no `spec.*` prefix) to signal that it is a derived index, not a real `PhotoSpec` field.
- Change `PhotoQuery.getSort()` and `PhotoPublicQuery.getSort()` to emit `effectiveTime` (instead of `exif.dateTimeOriginal`) as the primary sort key for effective-time ordering. The public **query parameter** name stays `exif.dateTimeOriginal` per the documented API contract — only the internal sort key is remapped.
- Remove `PhotoServiceImpl.listPhotoByEffectiveTime()` and `PhotoPublicQueryServiceImpl.listPhotosByEffectiveTime()` (and the `isEffectiveTimeSort()` branches that route into them). Both `listPhoto` and `listPhotos` now go through the standard `client.listBy(..., PageRequest)` path with the new sort key.
- Keep `PhotoSortUtils.effectiveTimeComparator(boolean)` as a reference / test utility — it is no longer used in the request hot path but documents the intended ordering and remains useful for unit tests.
- Keep the existing `exif.dateTimeOriginal` IndexSpec untouched (pure-EXIF semantics) so future features that genuinely need "EXIF time only" can still query it.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `photo-sort-options`: Effective-time sort SHALL be served by an index-backed query, not by loading every matching photo into memory. Observable sort order is unchanged.
- `public-api`: `GET /apis/api.photo.halo.run/v1alpha1/photos` default and `sort=exif.dateTimeOriginal,*` ordering SHALL be served by an index-backed query. Observable response shape and order are unchanged.

## Impact

- **Backend (Java)**:
  - `run.halo.photos.PhotoPlugin` — register the new `effectiveTime` IndexSpec.
  - `run.halo.photos.PhotoQuery` / `run.halo.photos.PhotoPublicQuery` — translate effective-time sort to `effectiveTime`.
  - `run.halo.photos.service.impl.PhotoServiceImpl` — remove `listPhotoByEffectiveTime` branch.
  - `run.halo.photos.finders.impl.PhotoPublicQueryServiceImpl` — remove `listPhotosByEffectiveTime` branch.
- **Frontend / Console UI**: no change. Sort dropdown labels and query values are unchanged.
- **API contract**: `sort=exif.dateTimeOriginal,(asc|desc)` continues to mean "effective time" with the documented fallback; clients see no change.
- **Data**: no migration needed. Halo recomputes `IndexSpec` values on plugin start, so the new `effectiveTime` index is populated automatically.
- **Performance**: effective-time-sorted list responses go from O(N) memory and ≥O(N log N) CPU per request to the same complexity as any other indexed sort (page-size-bounded).
