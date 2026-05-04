## 1. Register computed `effectiveTime` IndexSpec

- [x] 1.1 In `PhotoPlugin#start`, register `IndexSpecs.<Photo, String>single("effectiveTime", String.class)` with an `indexFunc` that returns `exif.dateTimeOriginal.toString()` when present, otherwise `metadata.creationTimestamp.toString()`, otherwise `""`.
- [x] 1.2 Keep the existing `exif.dateTimeOriginal` IndexSpec untouched (pure-EXIF semantics).

## 2. Translate effective-time sort key in query classes

- [x] 2.1 In `PhotoQuery.getSort()`, emit `desc("effectiveTime")` (instead of `desc(DATE_TIME_ORIGINAL_SORT)`) for the unsorted-default branch.
- [x] 2.2 In `PhotoQuery.getSort()`, when an explicit `sort` carries `exif.dateTimeOriginal`, rewrite the order's property to `effectiveTime` while preserving direction; leave other properties (`metadata.creationTimestamp`) untouched.
- [x] 2.3 Apply the same two changes to `PhotoPublicQuery.getSort()`.
- [x] 2.4 Confirm `PhotoQuery.buildParameters` / `PhotoPublicQuery.buildParameters` documentation continues to advertise `exif.dateTimeOriginal` as the public sort field name (no API contract change).

## 3. Remove in-memory effective-time branches

- [x] 3.1 Delete `PhotoServiceImpl.listPhotoByEffectiveTime(...)` and the `if (query.isEffectiveTimeSort())` short-circuit in `PhotoServiceImpl.listPhoto(...)`. The remaining body becomes a single `client.listBy(Photo.class, toListOptions(query), PageRequestImpl.of(query.getPage(), query.getSize(), query.getSort()))` call.
- [x] 3.2 Delete `PhotoPublicQueryServiceImpl.listPhotosByEffectiveTime(...)` and the corresponding short-circuit in `PhotoPublicQueryServiceImpl.listPhotos(...)`. The remaining body becomes the existing `client.listBy(...).flatMap(... toPhotoVo ...)` chain unconditionally.
- [x] 3.3 Verify `isEffectiveTimeSort()` and `isEffectiveTimeAscending()` on `PhotoQuery` / `PhotoPublicQuery` have no remaining callers, then delete them along with the `DATE_TIME_ORIGINAL_SORT` constant if no longer referenced. (Helper methods deleted; `DATE_TIME_ORIGINAL_SORT` retained because `getSort()` still uses it to translate the public sort key into the internal index name.)
- [x] 3.4 Remove the now-unused `PhotoSortUtils` import from `PhotoServiceImpl` and `PhotoPublicQueryServiceImpl`. (Removed in `PhotoServiceImpl`. Retained in `PhotoPublicQueryServiceImpl` because `listGroups` still uses `PhotoSortUtils.groupComparator()`.)

## 4. Update `PhotoFinderImpl` default sort

- [x] 4.1 In `PhotoFinderImpl.defaultPhotoSort()`, replace `desc("exif.dateTimeOriginal")` with `desc("effectiveTime")` so the SPI default ordering uses the new index. (Also updated the duplicate `defaultPhotoSort()` in `PhotoRouter` for consistency.)
- [x] 4.2 Confirm `PhotoFinderImpl.listAll()`, `PhotoFinderImpl.listBy(group)`, and `PhotoFinderImpl.groupBy()` still produce the expected order against a fixture (no behavior change expected, just verifying the new sort key flows through `listAllPhotos`). (Existing `PhotoFinderImplTest` mocks `PhotoPublicQueryService` so the renamed sort key flows through unchanged; no test edits required.)

## 5. Tests

- [x] 5.1 Add a unit test on the new index function: a `Photo` with EXIF time emits `dateTimeOriginal.toString()`; a `Photo` without EXIF time emits `metadata.creationTimestamp.toString()`; a `Photo` with neither emits `""`.
- [x] 5.2 Add a service-layer test that asserts `PhotoServiceImpl.listPhoto(query)` for the default sort returns photos ordered by effective time (with EXIF and non-EXIF photos interleaved by their effective time, not partitioned).
- [x] 5.3 Add a service-layer test that asserts `PhotoPublicQueryServiceImpl.listPhotos(options, page)` for both `Sort.by(desc("effectiveTime"))` and the default composite sort produces the same order as `PhotoSortUtils.effectiveTimeComparator(false)` over a fixture.
- [x] 5.4 Add an explicit assertion that no test exercises `client.listAll(Photo.class, ...)` for an effective-time-sorted request (i.e., the in-memory branch no longer runs).

## 6. Verify

- [x] 6.1 Run `./gradlew build` to confirm compilation.
- [x] 6.2 Run `./gradlew test` to ensure no regressions.
- [ ] 6.3 Manually verify in `haloServer` that `GET /apis/console.api.photo.halo.run/v1alpha1/photos?sort=exif.dateTimeOriginal,desc` and `GET /apis/api.photo.halo.run/v1alpha1/photos` return the same order as before, and that the response time on a large gallery is bounded by `pageSize` rather than by total photo count.
