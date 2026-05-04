## Context

Photos data is currently exposed in three places:

1. `console.api.photo.halo.run/v1alpha1` — auth-required, console-only.
2. `PhotoFinder` (`@Finder("photoFinder")`) — Java SPI for Thymeleaf templates; reads via `ReactiveExtensionClient`.
3. `PhotoRouter` — server-side rendered HTML routes at `/photos` and `/photos/{name}`; reads via `ReactiveExtensionClient` directly.

There is no anonymous JSON surface. Theme authors building React/Vue/Svelte galleries either screen-scrape SSR HTML or call internal endpoints. The companion plugin `plugin-moments` solved the same problem with a public API group `api.moment.halo.run/v1alpha1` plus a `MomentPublicQueryService` that fronts the data path; this design mirrors that pattern with photo-specific decisions.

## Goals / Non-Goals

**Goals:**
- Provide an anonymous, read-only JSON API at `api.photo.halo.run/v1alpha1` covering photos, photo groups, and tags.
- Establish a single read-path (`PhotoPublicQueryService`) used by the new endpoints, the existing `PhotoFinder`, and `PhotoRouter`, so the three surfaces cannot drift.
- Hide GPS coordinates from every theme-visible `PhotoVo` while keeping the data on the underlying `Photo` extension for future opt-in features (e.g. photo map).

**Non-Goals:**
- Visibility/approval/moderation predicate layer (no `visible`/`approved` fields on `Photo`).
- Time-range filtering (`startDate`/`endDate`).
- Inlining `photos[]` inside `/photogroups` responses.
- `permalink` field on `PhotoTagVo` (SPA consumers compose their own routes).
- Adding `tag` query support to the SSR `PhotoRouter`.
- Any write endpoints under `api.photo.halo.run`.
- Touching `console.api.photo.halo.run/v1alpha1`.

## Decisions

### D1 — URL naming follows kind plurals
Public endpoints are `/photos`, `/photogroups`, `/tags`, matching the existing `PhotoEndpoint`/`PhotoGroupEndpoint` plurals and the `MomentQueryEndpoint` precedent (`/moments`).

**Considered alternative:** shorter `/groups`. Rejected because it diverges from the `core.halo.run` plural (`photogroups`), would generate a less obvious OpenAPI operation name, and creates inconsistency with the console namespace.

### D2 — Single shared `PhotoPublicQueryService`
The new endpoints, the existing `PhotoFinder`, and `PhotoRouter` all read through one Spring component:

```
PhotoQueryEndpoint        ─┐
PhotoGroupQueryEndpoint   ─┤
PhotoFinderImpl           ─┼─► PhotoPublicQueryService ─► ReactiveExtensionClient
PhotoRouter (list/detail) ─┘
```

Service surface (interface in `run.halo.photos.finders` to mirror `MomentPublicQueryService`):
- `Mono<ListResult<PhotoVo>> listPhotos(ListOptions options, PageRequest page)`
- `Mono<PhotoVo> getByName(String name)` — returns `Mono.empty()` when missing or soft-deleted; the endpoint translates that to 404.
- `Mono<ListResult<PhotoGroupVo>> listGroups(ListOptions options, PageRequest page)` — does NOT populate `PhotoGroupVo.photos[]`.
- `Flux<PhotoTagVo> listTags(String nameFilter)` — returns distinct tag names with counts; optional case-insensitive `contains` filter.
- `Mono<PhotoVo> toPhotoVo(Photo photo)` — encapsulates the GPS-null transformation; **all** `PhotoVo`-producing callers must go through this.

**Considered alternative:** keep `PhotoFinder` independent and only add an endpoint. Rejected because today's `PhotoRouter` already calls `client.listAll(Photo.class, …)` directly, and we'd end up with three sort/filter implementations to keep in sync (especially the effective-time sort that needs in-memory comparison).

### D3 — Sort and filter parameters mirror existing `PhotoQuery`
`PhotoPublicQuery` extends `SortableRequest` (same as `PhotoQuery`/`MomentPublicQuery`) and supports:

| Parameter | Type | Notes |
| --- | --- | --- |
| `group` | String | Filter by `spec.groupName`. Ignored when `ungrouped=true`. |
| `ungrouped` | Boolean | When `true`, return only photos with `spec.groupName` empty/missing (matches existing index coercion `null → ""`). |
| `tag` | String | Filter by `spec.tags`. |
| `keyword` | String | Case-insensitive `contains` on `spec.displayName`. |
| `labelSelector`, `fieldSelector` | String | Standard Halo selector syntax. |
| `sort` | String | `field,(asc|desc)`. Supported fields: `spec.dateTimeOriginal`, `metadata.creationTimestamp`. |
| `page`, `size` | Integer | Standard pagination. |

Default sort matches `PhotoQuery`: `spec.dateTimeOriginal desc → metadata.creationTimestamp desc → metadata.name asc`. The effective-time sort uses the in-memory comparator (`PhotoSortUtils.effectiveTimeComparator`) because `spec.dateTimeOriginal` may be null.

**Considered alternative:** include `startDate`/`endDate` (per `MomentPublicQuery`). Out of scope for this change; can be added later without breaking existing consumers.

### D4 — `PhotoTagVo` shape: `{ name, photoCount }` only
No `permalink`. SPA themes compose their own URLs; an SSR-friendly link would be misleading anyway because the SSR `PhotoRouter` does not yet support `?tag=`.

`photoCount` is computed by counting matches on the indexed `spec.tags` field, mirroring the per-tag aggregation pattern in `MomentFinderImpl.listAllTags`.

### D5 — `/photogroups` does not embed photos
Returns `ListResult<PhotoGroupVo>` with each group carrying `metadata`, `spec`, and `status.photoCount`. Callers that need photos for a group call `GET /photos?group=<name>`.

This matches the existing console `PhotoGroupEndpoint` shape and keeps `/photogroups` cheap (no per-group N+1).

The existing `PhotoFinder.groupBy()` SPI continues to populate `photos[]` because Thymeleaf templates rely on that shape; that path uses `PhotoPublicQueryService.listPhotos(group=…)` internally to fill the array.

### D6 — GPS hidden via null in `PhotoVo`, not removed from the type
`PhotoVo.from(Photo)` clones `Photo.PhotoExif` and sets `gpsLatitude`, `gpsLongitude`, `gpsAltitude` to `null`. The clone prevents mutating the input `Photo` — important because callers like `PhotoRouter.detailHandler` reuse the same `Photo` instance for navigation context.

The fields stay on `Photo.PhotoExif`, so:
- Console (admin) consumers, which serialize `Photo` directly, still see GPS.
- The data is preserved on disk for future opt-in features (e.g. photo map).

We rely on Halo's existing Jackson `Include.NON_NULL` configuration to omit nulled fields from JSON output. A unit test pins this assumption (D9).

**Considered alternative:** introduce a `PhotoExifVo` without GPS fields. Rejected because the user wants the door open for future GPS use cases; a parallel narrower type would force a schema migration to undo.

### D7 — Anonymous role uses `aggregate-to-anonymous`
A new `role-template-photos-anonymous` carries `rbac.authorization.halo.run/aggregate-to-anonymous: "true"` plus `halo.run/hidden: "true"` (so it doesn't appear in the UI), and grants `get`/`list` on `apiGroups: ["api.photo.halo.run"]`, `resources: ["photos", "photogroups", "tags"]`. This matches `role-template-moments-anonymous`.

The anonymous role MUST NOT grant access to `core.halo.run` or `console.api.photo.halo.run` resources.

### D8 — Endpoint split: photo-side vs group-side
Two `CustomEndpoint` beans under the same `api.photo.halo.run/v1alpha1` group:
- `PhotoQueryEndpoint` → `/photos`, `/photos/{name}`, `/tags` (OpenAPI tag `api.photo.halo.run/v1alpha1/Photo`)
- `PhotoGroupQueryEndpoint` → `/photogroups` (OpenAPI tag `api.photo.halo.run/v1alpha1/PhotoGroup`)

Co-locating `/tags` with `/photos` (rather than its own endpoint class) keeps the OpenAPI tag count low while still grouping group-related operations separately, matching the existing console split.

### D9 — Tests pin the GPS-null assumption
Beyond unit tests for the service, a focused test serializes a `PhotoVo` produced from a `Photo` with full GPS data and asserts the resulting JSON does not contain `gpsLatitude`, `gpsLongitude`, or `gpsAltitude` (whether by null-omission or absence). This guards against a future Jackson config change silently re-leaking GPS.

## Risks / Trade-offs

- **Risk: anonymous data exposure beyond what admins expect.** → Mitigation: anonymous role is scoped to `api.photo.halo.run` only and to `get`/`list` verbs; GPS is nulled in `PhotoVo`; no write verbs exist on the public group.
- **Risk: GPS-null is a behavior change for SSR themes that render maps.** → Mitigation: called out in the proposal's Impact section; restoring map use cases is a future change with a deliberate opt-in path (e.g. an admin-scoped finder method or a `?withGps=true` parameter requiring auth).
- **Risk: refactoring `PhotoRouter` and `PhotoFinder` regresses existing SSR pages.** → Mitigation: route tests for `/photos`, `/photos/{name}`, `/photos/page/{n}` redirect, and finder method behavior must pass before merge; no changes to template models or URL strings.
- **Trade-off: in-memory effective-time sort caps practical photo counts.** → The existing `PhotoQuery`/`PhotoServiceImpl` already accepts this trade-off; the new service inherits it. Index-backed `dateTimeOriginal` sort is a separate future change.
- **Trade-off: tag listing computes counts per-request.** → Acceptable at expected gallery sizes (hundreds to low thousands). If gallery sizes grow, a denormalized counter becomes a separate change; the public `PhotoTagVo` shape does not change.

## Migration Plan

1. Land all backend changes (new service, endpoints, role, `PhotoVo.from` change) and the refactor of `PhotoFinder`/`PhotoRouter` together.
2. Run `./gradlew generateApiClient` and commit the regenerated TypeScript client.
3. No data migration required — `Photo` and `PhotoGroup` schemas are unchanged.
4. **Rollback**: revert the change. No persisted state changes mean no data cleanup. Consumers of the new API simply lose the endpoints; SSR/Finder behavior returns to its current GPS-visible state.

## Open Questions

None. URL naming, group payload shape, predicate layer, GPS handling, time-range filtering, finder/router refactor scope, and `PhotoTagVo` shape are all decided.
