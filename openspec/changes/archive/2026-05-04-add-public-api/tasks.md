## 1. Foundational Types

- [x] 1.1 Create `run.halo.photos.vo.PhotoTagVo` (`@Value @Builder`) with fields `name: String`, `photoCount: Integer` — no `permalink` field
- [x] 1.2 Create `run.halo.photos.PhotoPublicQuery` extending `SortableRequest`, exposing `group`, `ungrouped`, `tag`, `keyword`, default sort matching `PhotoQuery`, plus a `buildParameters` static helper for `springdoc` registration
- [x] 1.3 Add a unit test asserting `PhotoPublicQuery` parses `group`/`ungrouped`/`tag`/`keyword`/`sort`/`page`/`size` from a `ServerWebExchange`

## 2. Hide GPS In PhotoVo

- [x] 2.1 Modify `PhotoVo.from(Photo)` to construct a defensive copy of `Photo.PhotoExif` (clone all fields) and force `gpsLatitude`, `gpsLongitude`, `gpsAltitude` to `null` on the copy
- [x] 2.2 Verify the source `Photo` instance is not mutated — write a unit test that takes a `Photo` with non-null GPS, calls `PhotoVo.from`, and asserts the original `photo.exif.gpsLatitude/Longitude/Altitude` are still non-null
- [x] 2.3 Write a test serializing a `PhotoVo` (built via `PhotoVo.from` from a GPS-bearing `Photo`) with the application's `ObjectMapper` and asserting the resulting JSON does NOT contain `gpsLatitude`, `gpsLongitude`, or `gpsAltitude` keys
- [x] 2.4 If the JSON test in 2.3 fails because Jackson does not omit nulls by default in this context, add an `@JsonInclude(JsonInclude.Include.NON_NULL)` annotation scoped to `Photo.PhotoExif` (or to `PhotoVo` if narrower scope is preferred) and re-run — test passed without annotation (Halo's ObjectMapper uses NON_NULL globally)

## 3. PhotoPublicQueryService

- [x] 3.1 Create interface `run.halo.photos.finders.PhotoPublicQueryService` with the five methods listed in design.md D2
- [x] 3.2 Implement `run.halo.photos.finders.impl.PhotoPublicQueryServiceImpl` (`@Component`):
  - `listPhotos(ListOptions, PageRequest)` — uses the existing effective-time comparator from `PhotoSortUtils` when sort is unset or targets `spec.dateTimeOriginal`; otherwise delegates to `client.listBy(...)`
  - `getByName(String)` — `client.fetch` + filter `!photo.isDeleted()` + `toPhotoVo`
  - `listGroups(ListOptions, PageRequest)` — reuses the priority-aware sort from `PhotoGroupServiceImpl` and populates `status.photoCount` for each group, but does NOT call `listBy` to fill `photos[]`
  - `listTags(String nameFilter)` — reads photos via the indexed `spec.tags` field, groups by tag name, returns `PhotoTagVo` per tag with case-insensitive `contains` filtering on `nameFilter`
  - `toPhotoVo(Photo)` — wraps `PhotoVo.from(...)` (so a single seam exists for future enrichment)
- [x] 3.3 Unit test `listPhotos` for: group filter, ungrouped filter, tag filter, keyword filter, default sort ordering when EXIF time is null, pagination
- [x] 3.4 Unit test `getByName` returns empty for soft-deleted photos
- [x] 3.5 Unit test `listGroups` does NOT populate `PhotoGroupVo.photos[]` and that `status.photoCount` matches the actual photo count
- [x] 3.6 Unit test `listTags` returns counts that match the underlying `Photo.spec.tags` data and applies the case-insensitive substring filter

## 4. Public Endpoints

- [x] 4.1 Create `run.halo.photos.PhotoQueryEndpoint` (`@Component implements CustomEndpoint`) with `groupVersion()` returning `api.photo.halo.run/v1alpha1`; routes `GET /photos`, `GET /photos/{name}`, `GET /tags`; OpenAPI tag `api.photo.halo.run/v1alpha1/Photo`
- [x] 4.2 Wire `PhotoQueryEndpoint.listPhotos` to `PhotoPublicQueryService.listPhotos(query.toListOptions(), query.toPageRequest())` using a new `PhotoPublicQuery`
- [x] 4.3 Wire `PhotoQueryEndpoint.getPhoto` to `getByName`, returning 404 via `Mono.empty()` mapping to `ServerResponse.notFound().build()`
- [x] 4.4 Wire `PhotoQueryEndpoint.listTags` to `PhotoPublicQueryService.listTags(name)` reading the optional `name` query param
- [x] 4.5 Create `run.halo.photos.PhotoGroupQueryEndpoint` (`@Component implements CustomEndpoint`) under the same group; route `GET /photogroups`; OpenAPI tag `api.photo.halo.run/v1alpha1/PhotoGroup`; delegates to `PhotoPublicQueryService.listGroups`
- [ ] 4.6 Verify both endpoints are picked up by Halo's `CustomEndpoint` registration (no extra wiring required) by hitting `GET /apis/api.photo.halo.run/v1alpha1/photos` from a manual smoke test — requires running Halo server

## 5. Anonymous Role

- [x] 5.1 Append a new YAML document for `role-template-photos-anonymous` to `src/main/resources/extensions/roleTemplate.yaml` with labels `halo.run/role-template: "true"`, `halo.run/hidden: "true"`, `rbac.authorization.halo.run/aggregate-to-anonymous: "true"`
- [x] 5.2 Rules: `apiGroups: ["api.photo.halo.run"]`, `resources: ["photos", "photogroups", "tags"]`, `verbs: ["get", "list"]`
- [ ] 5.3 Verify anonymous reachability: stop the dev server, restart, hit each public endpoint without auth — expect 200; hit any console endpoint without auth — expect 401/403 — requires running Halo server

## 6. Refactor PhotoFinder Onto The Service

- [x] 6.1 Inject `PhotoPublicQueryService` into `PhotoFinderImpl`; remove the direct `ReactiveExtensionClient` dependency where it is now redundant
- [x] 6.2 Reimplement `listAll()`, `list(page,size)`, `list(page,size,group)`, and `listBy(group)` on top of `PhotoPublicQueryService.listPhotos(...)`; preserve return types and ordering
- [x] 6.3 Reimplement `groupBy()` on top of `PhotoPublicQueryService.listGroups(...)` PLUS `PhotoPublicQueryService.listPhotos(group=...)` per group to populate `PhotoGroupVo.photos[]` (this is the SSR-shaped helper, distinct from the public `/photogroups` endpoint)
- [x] 6.4 Add a regression test asserting `photoFinder.groupBy()` still emits `PhotoGroupVo` instances with non-empty `photos` arrays for groups that have photos
- [x] 6.5 Add a regression test asserting `photoFinder.listAll()` returns the same set of photos (by `metadata.name`) in the same order as before the refactor

## 7. Refactor PhotoRouter Onto The Service

- [x] 7.1 Replace `PhotoRouter`'s direct `client.listAll(Photo.class, …)` calls in `listHandler` and `loadFilteredPhotos` with `PhotoPublicQueryService` calls; preserve the `UrlContextListResult`/neighbor-window logic
- [x] 7.2 Keep `client.fetch(Photo.class, name)` in `detailHandler` only if needed for the soft-delete filter; otherwise route through `PhotoPublicQueryService.getByName`
- [ ] 7.3 Verify URLs unchanged: `GET /photos`, `GET /photos?group=trips&page=2&size=20`, `GET /photos/{name}`, `GET /photos/page/3` redirect, `GET /photos/{name}?group=other` redirect when the photo's group does not match — requires running Halo server
- [ ] 7.4 Verify rendered model attributes unchanged: `groups`, `photos`, `title`, `photoUrl`, template id for the list page; `photo`, `neighbors`, `prev`, `next`, `position`, `total`, `group`, `page`, `size`, `backUrl`, `title`, `photoUrl`, template id for the detail page — requires running Halo server

## 8. Endpoint-Level Tests

- [x] 8.1 Integration test for `GET /apis/api.photo.halo.run/v1alpha1/photos`: anonymous status 200, response is `ListResult<PhotoVo>`, GPS fields absent in JSON (tested via endpoint-level unit test with WebTestClient)
- [x] 8.2 Integration test for `GET /apis/api.photo.halo.run/v1alpha1/photos/{name}`: 200 for existing photo, 404 for missing, 404 for soft-deleted
- [x] 8.3 Integration test for `GET /apis/api.photo.halo.run/v1alpha1/photogroups`: anonymous status 200, response items have null/empty `photos`, `status.photoCount` matches reality
- [x] 8.4 Integration test for `GET /apis/api.photo.halo.run/v1alpha1/tags`: distinct tag names with counts, optional `name` filter works case-insensitively, no `permalink` key in JSON
- [x] 8.5 Negative test: `POST /apis/api.photo.halo.run/v1alpha1/photos` with anonymous credentials returns 4xx (no route match)
- [ ] 8.6 Negative test: anonymous `GET /apis/console.api.photo.halo.run/v1alpha1/photos` still returns 401/403 (anonymous role does not leak into console) — requires running Halo server

## 9. Generated Client And Docs

- [x] 9.1 Run `./gradlew generateApiClient`
- [x] 9.2 Inspect `console/src/api/generated/` for new public-API model and operation classes; commit the regenerated files (do not hand-edit)
- [x] 9.3 Update `README.md` with a "Public API" section listing the four endpoints, the anonymous role, the GPS-hidden behavior, and the SPA-consumer use case

## 10. Final Verification

- [x] 10.1 Run `./gradlew test` and confirm all backend tests pass
- [x] 10.2 Run `cd console && pnpm type-check` and confirm no TypeScript errors after client regeneration
- [x] 10.3 Run `./gradlew build` for a full plugin build
- [ ] 10.4 Manual smoke test against `./gradlew haloServer`: hit each public endpoint anonymously with `curl`, verify GPS absent from `/photos` response, verify `/photogroups` returns no inline `photos`, verify `/tags?name=` filters correctly — requires running Halo server
