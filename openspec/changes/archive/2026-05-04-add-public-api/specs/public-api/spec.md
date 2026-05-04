## ADDED Requirements

### Requirement: Public API Group Exists At api.photo.halo.run/v1alpha1

The plugin SHALL expose a custom API group `api.photo.halo.run/v1alpha1` separate from `console.api.photo.halo.run/v1alpha1`. The public group SHALL contain only read-only endpoints (`get`/`list` verbs); no `create`/`update`/`delete`/`deletecollection` endpoints SHALL be exposed under this group.

#### Scenario: Public group is reachable

- **WHEN** an anonymous client issues `GET /apis/api.photo.halo.run/v1alpha1/photos`
- **THEN** the request is routed to the public photo endpoint (not the console endpoint)

#### Scenario: Write verbs are not exposed

- **WHEN** any client issues `POST`, `PUT`, `PATCH`, or `DELETE` against any path under `/apis/api.photo.halo.run/v1alpha1/`
- **THEN** the request fails with a 4xx (no matching route)

---

### Requirement: Anonymous Users Can Read Public Photo Endpoints

The plugin SHALL register a role template named `role-template-photos-anonymous` that carries the labels `halo.run/role-template: "true"`, `halo.run/hidden: "true"`, and `rbac.authorization.halo.run/aggregate-to-anonymous: "true"`. The role's rules SHALL grant `get` and `list` verbs on `resources: ["photos", "photogroups", "tags"]` under `apiGroups: ["api.photo.halo.run"]` and SHALL NOT grant any verbs on `core.halo.run` or `console.api.photo.halo.run`.

#### Scenario: Anonymous list photos

- **WHEN** an unauthenticated client issues `GET /apis/api.photo.halo.run/v1alpha1/photos`
- **THEN** the response status is 200 and the body is a `ListResult<PhotoVo>`

#### Scenario: Anonymous cannot reach console endpoints via aggregation

- **WHEN** an unauthenticated client issues `GET /apis/console.api.photo.halo.run/v1alpha1/photos`
- **THEN** the response status is 401 or 403

#### Scenario: Anonymous cannot create photos

- **WHEN** an unauthenticated client issues `POST` to any path under `/apis/api.photo.halo.run/v1alpha1/`
- **THEN** the response status is 4xx (route not found or method not allowed)

---

### Requirement: GET /photos Lists Photos With Filters And Pagination

The endpoint `GET /apis/api.photo.halo.run/v1alpha1/photos` SHALL accept the following query parameters and return `ListResult<PhotoVo>`:

| Parameter | Type | Behavior |
| --- | --- | --- |
| `group` | String | Restrict to photos whose `spec.groupName` equals this value. Ignored when `ungrouped=true`. |
| `ungrouped` | Boolean | When `true`, restrict to photos whose `spec.groupName` is empty or unset. Overrides `group`. |
| `tag` | String | Restrict to photos whose `spec.tags` contains this value. |
| `keyword` | String | Case-insensitive `contains` match on `spec.displayName`. |
| `labelSelector` | String | Standard Halo label selector syntax. |
| `fieldSelector` | String | Standard Halo field selector syntax. |
| `sort` | String | `field,(asc|desc)`. Supported fields: `spec.dateTimeOriginal`, `metadata.creationTimestamp`. |
| `page` | Integer | 1-based page number. |
| `size` | Integer | Page size. |

When `sort` is omitted, the default order SHALL be `spec.dateTimeOriginal desc → metadata.creationTimestamp desc → metadata.name asc`. Photos with a missing `spec.dateTimeOriginal` SHALL fall back to `metadata.creationTimestamp` for the primary order.

#### Scenario: Filter by group

- **WHEN** an anonymous client issues `GET /apis/api.photo.halo.run/v1alpha1/photos?group=trips`
- **THEN** the response contains only photos whose `spec.groupName` equals `trips`

#### Scenario: Filter ungrouped photos

- **WHEN** an anonymous client issues `GET /apis/api.photo.halo.run/v1alpha1/photos?ungrouped=true&group=trips`
- **THEN** the response contains only photos whose `spec.groupName` is empty or missing
- **THEN** the `group=trips` parameter is ignored

#### Scenario: Filter by tag

- **WHEN** an anonymous client issues `GET /apis/api.photo.halo.run/v1alpha1/photos?tag=sunset`
- **THEN** the response contains only photos whose `spec.tags` array contains `sunset`

#### Scenario: Default ordering

- **WHEN** an anonymous client issues `GET /apis/api.photo.halo.run/v1alpha1/photos` with no `sort` parameter
- **THEN** photos with a populated `spec.dateTimeOriginal` precede photos without one
- **THEN** within each group of photos sharing the same effective time, ordering is by `metadata.creationTimestamp desc`, then `metadata.name asc`

#### Scenario: Pagination

- **WHEN** an anonymous client issues `GET /apis/api.photo.halo.run/v1alpha1/photos?page=2&size=10`
- **THEN** the response `ListResult` reports `page=2`, `size=10`, and the items array contains at most 10 entries

---

### Requirement: GET /photos/{name} Returns A Single Photo

The endpoint `GET /apis/api.photo.halo.run/v1alpha1/photos/{name}` SHALL return a `PhotoVo` for the photo with the given `metadata.name` when it exists and is not soft-deleted, and SHALL return `404 Not Found` otherwise.

#### Scenario: Existing photo is returned

- **WHEN** an anonymous client issues `GET /apis/api.photo.halo.run/v1alpha1/photos/abc` and a photo with `metadata.name=abc` exists and is not soft-deleted
- **THEN** the response status is 200 and the body is a `PhotoVo` whose `metadata.name` is `abc`

#### Scenario: Missing photo returns 404

- **WHEN** an anonymous client issues `GET /apis/api.photo.halo.run/v1alpha1/photos/does-not-exist`
- **THEN** the response status is 404

#### Scenario: Soft-deleted photo returns 404

- **WHEN** an anonymous client issues `GET /apis/api.photo.halo.run/v1alpha1/photos/{name}` for a photo whose `metadata.deletionTimestamp` is set
- **THEN** the response status is 404

---

### Requirement: GET /photogroups Lists Groups Without Inline Photos

The endpoint `GET /apis/api.photo.halo.run/v1alpha1/photogroups` SHALL return `ListResult<PhotoGroupVo>` with each `PhotoGroupVo` carrying only `metadata`, `spec`, and `status.photoCount`. The `PhotoGroupVo.photos` array SHALL NOT be populated by this endpoint (it is null or empty).

The endpoint SHALL accept `labelSelector`, `fieldSelector`, `sort`, `page`, and `size` query parameters. Default ordering SHALL be `spec.priority desc → metadata.creationTimestamp desc → metadata.name asc`.

#### Scenario: Group payload omits photos

- **WHEN** an anonymous client issues `GET /apis/api.photo.halo.run/v1alpha1/photogroups`
- **THEN** every item's `photos` field is null or absent in the JSON
- **THEN** every item's `status.photoCount` reflects the current count of non-deleted photos in that group

#### Scenario: Default ordering reflects priority

- **WHEN** an anonymous client issues `GET /apis/api.photo.halo.run/v1alpha1/photogroups` with no `sort` parameter
- **THEN** groups with a higher numeric `spec.priority` appear before groups with a lower priority
- **THEN** ties on priority break by `metadata.creationTimestamp desc`, then `metadata.name asc`

---

### Requirement: GET /tags Lists Distinct Tag Names With Counts

The endpoint `GET /apis/api.photo.halo.run/v1alpha1/tags` SHALL return `List<PhotoTagVo>` where each entry has shape `{ name: String, photoCount: Integer }`. Tags SHALL be the set of distinct values across all `Photo.spec.tags` arrays, excluding soft-deleted photos. `photoCount` SHALL be the number of non-deleted photos whose `spec.tags` contains that tag name.

The endpoint SHALL accept an optional `name` query parameter. When `name` is non-blank, the response SHALL include only entries whose tag name contains the parameter value (case-insensitive); when `name` is absent or blank, all distinct tags SHALL be returned.

`PhotoTagVo` SHALL NOT include a `permalink` field.

#### Scenario: List all tags

- **WHEN** an anonymous client issues `GET /apis/api.photo.halo.run/v1alpha1/tags`
- **THEN** the response contains one entry per distinct tag name across all non-deleted photos
- **THEN** every entry has a non-null `photoCount` matching the number of non-deleted photos that carry that tag

#### Scenario: Filter tags by name substring

- **WHEN** an anonymous client issues `GET /apis/api.photo.halo.run/v1alpha1/tags?name=sun` and the gallery has tags `sunset`, `sunrise`, `mountain`
- **THEN** the response contains only the entries for `sunset` and `sunrise`

#### Scenario: Tag VO has no permalink

- **WHEN** any consumer deserializes a `PhotoTagVo` returned by this endpoint
- **THEN** the JSON object has no `permalink` key

---

### Requirement: PhotoPublicQueryService Is The Single Read-Path For Public Photo Data

The plugin SHALL provide a Spring component `PhotoPublicQueryService` that exposes the following methods, and the public endpoints, the `PhotoFinder` SPI implementation, and the SSR `PhotoRouter` SHALL all read photo data through it (not directly through `ReactiveExtensionClient`):

- `Mono<ListResult<PhotoVo>> listPhotos(ListOptions options, PageRequest page)`
- `Mono<PhotoVo> getByName(String name)` — returns `Mono.empty()` when the photo is missing or soft-deleted
- `Mono<ListResult<PhotoGroupVo>> listGroups(ListOptions options, PageRequest page)` — does NOT populate `PhotoGroupVo.photos`
- `Flux<PhotoTagVo> listTags(String nameFilter)` — `nameFilter` is the optional case-insensitive substring filter
- `Mono<PhotoVo> toPhotoVo(Photo photo)` — applies the GPS-hiding transformation defined in the `photo-exif` capability

#### Scenario: Endpoint, finder, and router share the service

- **WHEN** any of the public endpoints, any `PhotoFinder` method that returns `PhotoVo`, or the SSR `PhotoRouter` produces photo data for a consumer
- **THEN** the data flows through `PhotoPublicQueryService` (no caller bypasses it by calling `ReactiveExtensionClient.listAll(Photo.class, …)` or `client.fetch(Photo.class, …)` for the purpose of building `PhotoVo`)

#### Scenario: getByName treats soft-deleted as absent

- **WHEN** a caller invokes `PhotoPublicQueryService.getByName(name)` and the matching photo has `metadata.deletionTimestamp` set
- **THEN** the returned `Mono` is empty (the soft-deleted photo is not exposed)

---

### Requirement: PhotoFinder Behavior Is Preserved After Refactor

The existing `PhotoFinder` interface SHALL retain its current methods (`listAll()`, `list(Integer, Integer)`, `list(Integer, Integer, String)`, `listBy(String)`, `groupBy()`) with unchanged signatures and observable behavior, except that GPS fields in returned `PhotoVo` instances are now null per the modified `photo-exif` capability. `PhotoFinder.groupBy()` SHALL continue to populate `PhotoGroupVo.photos[]` (this SPI method is distinct from the public `/photogroups` endpoint).

#### Scenario: Finder listAll still returns full gallery

- **WHEN** a Thymeleaf template calls `photoFinder.listAll()`
- **THEN** the returned `Flux<PhotoVo>` contains every non-deleted photo in the same order as before the refactor

#### Scenario: Finder groupBy still embeds photos

- **WHEN** a Thymeleaf template calls `photoFinder.groupBy()`
- **THEN** every emitted `PhotoGroupVo` has its `photos` array populated with the photos belonging to that group

---

### Requirement: PhotoRouter SSR Output Is Preserved After Refactor

The SSR routes `GET /photos`, `GET /photos/{name}`, and the legacy redirect `GET /photos/page/{page}` SHALL produce the same template models, URL parameters, and HTTP status codes as before the refactor. Internal data access SHALL go through `PhotoPublicQueryService` rather than calls to `ReactiveExtensionClient` made directly inside `PhotoRouter`.

#### Scenario: List page model unchanged

- **WHEN** an anonymous visitor requests `GET /photos?group=trips&page=2&size=20`
- **THEN** the rendered model contains the same attributes (`groups`, `photos`, `title`, `photoUrl`, template id) as before the refactor

#### Scenario: Detail page neighbors still computed

- **WHEN** an anonymous visitor requests `GET /photos/{name}`
- **THEN** the rendered model contains `neighbors`, `prev`, `next`, `position`, `total` consistent with the photo's position in the (group-filtered, effective-time-sorted) list

#### Scenario: Legacy pagination redirect preserved

- **WHEN** an anonymous visitor requests `GET /photos/page/3?group=trips`
- **THEN** the response is `301 Moved Permanently` with `Location: /photos?page=3&group=trips`
