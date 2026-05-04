## Why

Halo theme authors building client-rendered (React/Vue/Svelte) photo galleries currently have no JSON API to consume. The console endpoints under `console.api.photo.halo.run` require auth and are not intended for public use, and the SSR `PhotoRouter` only emits Thymeleaf-rendered HTML. This forces SPA-style themes to either roundtrip through the SSR page or call internal APIs they shouldn't depend on.

A public, anonymous, read-only JSON API at `api.photo.halo.run/v1alpha1` removes that gap. As a follow-on, `PhotoFinder` and `PhotoRouter` get refactored onto the same data path so SSR, finder-driven templates, and SPA themes all read photos through one service — preventing future drift.

## What Changes

- Add new public API group `api.photo.halo.run/v1alpha1` with read-only endpoints:
  - `GET /photos` — list photos with `group`, `ungrouped`, `tag`, `keyword`, `labelSelector`, `fieldSelector`, `sort`, `page`, `size`
  - `GET /photos/{name}` — fetch a single photo
  - `GET /photogroups` — list groups (metadata + spec + `status.photoCount` only; no inline photos)
  - `GET /tags` — list distinct tag names with photo counts; optional `name` filter
- Add an anonymous role template (`role-template-photos-anonymous`) that aggregates to anonymous and grants `get`/`list` on `photos`, `photogroups`, and `tags` under `api.photo.halo.run`.
- Add `PhotoTagVo` (`{ name, photoCount }`) and `PhotoPublicQuery` request shape.
- Introduce a new `PhotoPublicQueryService` that is the single read-path for all public photo queries.
- **BREAKING (theme-visible behavior)**: `PhotoVo.exif.gpsLatitude`, `gpsLongitude`, and `gpsAltitude` are now nulled in every `PhotoVo` produced by `PhotoVo.from(Photo)`. This affects the new public API, the existing `PhotoFinder`, and the existing `PhotoRouter`. The underlying `Photo.PhotoExif` type and stored data are unchanged; console (admin) consumers still see GPS data.
- Refactor `PhotoFinderImpl` to delegate to `PhotoPublicQueryService` (interface and observable behavior preserved, except for the GPS-null change above).
- Refactor `PhotoRouter` list/detail handlers to consume `PhotoPublicQueryService` instead of calling `ReactiveExtensionClient.listAll` directly. URL behavior and rendered output are unchanged.

## Capabilities

### New Capabilities
- `public-api`: Public, anonymous, read-only JSON API at `api.photo.halo.run/v1alpha1` plus the shared `PhotoPublicQueryService` that backs it. Covers endpoints, anonymous role, query parameters, response shapes (`PhotoVo`/`PhotoGroupVo`/`PhotoTagVo`), and the contract that this service is the single read-path for public photo data.

### Modified Capabilities
- `photo-exif`: The existing requirement "PhotoVo exposes the exif field for theme consumption" is narrowed — `PhotoVo.exif` no longer mirrors `Photo.exif` 1:1. GPS coordinates (`gpsLatitude`, `gpsLongitude`, `gpsAltitude`) are forced to null in `PhotoVo` for all consumers, even though they remain populated on `Photo.exif`.

## Impact

- **Affected code**:
  - `src/main/java/run/halo/photos/vo/PhotoVo.java` — modify `from(Photo)` to defensively clone `PhotoExif` and null the three GPS fields
  - `src/main/java/run/halo/photos/finders/impl/PhotoFinderImpl.java` — delegate to new service
  - `src/main/java/run/halo/photos/PhotoRouter.java` — switch to new service for list/detail
  - New: `PhotoPublicQuery`, `PhotoPublicQueryService` (+ impl), `PhotoQueryEndpoint`, `PhotoGroupQueryEndpoint`, `PhotoTagVo`
  - `src/main/resources/extensions/roleTemplate.yaml` — add anonymous role
- **Affected APIs**:
  - New: `api.photo.halo.run/v1alpha1` namespace (anonymous-readable)
  - Console API (`console.api.photo.halo.run/v1alpha1`) and standard CRUD (`core.halo.run/v1alpha1`) endpoints unchanged
- **Themes**:
  - SSR themes using `photoFinder` see `PhotoVo.exif.gpsLatitude/Longitude/Altitude` as `null` even when the original photo has GPS. Themes that render maps from finder output today will stop showing pins after this change. Re-enabling map use cases is explicitly out of scope and would be a future change.
- **Console frontend**: regenerate the TypeScript client via `./gradlew generateApiClient` so generated types reflect the new public endpoints; no console behavior change required.
- **Out of scope**: visibility/approval predicate layer, `startDate`/`endDate` filtering, inline `photos[]` in `/photogroups`, `permalink` on `PhotoTagVo`, adding `tag` query support to the SSR `PhotoRouter`, and any write endpoints under `api.photo.halo.run`.
