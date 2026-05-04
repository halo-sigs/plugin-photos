## ADDED Requirements

### Requirement: PhotoVo Carries A Stable Permalink

`PhotoVo` SHALL expose a `permalink` field whose value is the bare detail URL `/photos/{metadata.name}`. The factory `PhotoVo.from(Photo)` MUST populate this field for every `PhotoVo` it creates, regardless of the calling site (finder method, router, or other consumer).

The permalink MUST NOT include any context query parameters; it represents the canonical, context-free URL of the photo.

#### Scenario: Finder result includes permalink

- **WHEN** a theme calls any `PhotoFinder` method that returns `PhotoVo` instances (`listAll`, `list`, `listBy`, `groupBy`)
- **THEN** every returned `PhotoVo` has `permalink` set to `"/photos/" + metadata.name`

#### Scenario: Permalink is context-free

- **WHEN** a `PhotoVo` is consumed in any rendering context (list page, detail page, or finder caller in another plugin)
- **THEN** `photo.permalink` is the same string `/photos/{metadata.name}` and contains no `?group=`, `?page=`, or other query parameters

### Requirement: Per-Request URL Helper Bean Available To Templates

The list and detail handlers SHALL place a per-request URL helper under the model attribute `photoUrl`. The helper MUST hold the current `ServerRequest` (or its query-parameter map) so it can preserve context when constructing URLs.

The helper MUST expose the following methods:

- `detail(PhotoVo photo)`: returns `/photos/{name}` followed by the current request's whitelisted context parameters
- `detail(PhotoVo photo, Map<String, ?> overrides)`: same as above, but the supplied overrides MUST be applied after the request's parameters, replacing any conflicting values
- `list()`: returns the bare list URL `/photos`
- `list(String group)`: returns `/photos?group={group}`, omitting the `group` parameter when the argument is null or blank
- `list(String group, int page, int size)`: returns the full list URL including pagination, omitting any blank or non-positive parameters

The whitelisted context parameters MUST currently be `group`, `page`, and `size`. The helper MUST NOT propagate query parameters outside this whitelist when constructing detail URLs.

#### Scenario: Detail link from list page preserves group context

- **WHEN** a visitor is on `/photos?group=trips&page=2&size=20` and the template invokes `${photoUrl.detail(photo)}` for a photo with `metadata.name=abc`
- **THEN** the resulting URL is `/photos/abc?group=trips&page=2&size=20`

#### Scenario: Detail link drops blank group parameter

- **WHEN** a visitor is on `/photos?page=2` (no group filter) and the template invokes `${photoUrl.detail(photo)}` for a photo with `metadata.name=abc`
- **THEN** the resulting URL is `/photos/abc?page=2` and contains no `group=` parameter

#### Scenario: Detail link with overrides

- **WHEN** the template invokes `${photoUrl.detail(photo, {'group': 'other'})}` while the request is `/photos?group=trips&page=2`
- **THEN** the resulting URL has `group=other&page=2`, with the override replacing the request's `group`

#### Scenario: List link from detail page

- **WHEN** the detail page template invokes `${photoUrl.list(group)}` and `group` is `trips`
- **THEN** the resulting URL is `/photos?group=trips`

#### Scenario: List link from detail page with no active group

- **WHEN** the detail page template invokes `${photoUrl.list(group)}` and `group` is null or blank
- **THEN** the resulting URL is `/photos`

#### Scenario: List link with full pagination

- **WHEN** the detail page template invokes `${photoUrl.list(group, page, size)}` with `group="trips"`, `page=2`, `size=20`
- **THEN** the resulting URL is `/photos?group=trips&page=2&size=20`

### Requirement: Helper Bean Does Not Propagate Non-Whitelisted Parameters

When constructing URLs from the current request, the helper MUST NOT carry over query parameters that are outside the documented whitelist (currently `group`, `page`, `size`). This preserves the helper as a single place to manage the context contract; arbitrary URL parameters do not leak between requests.

#### Scenario: Unknown query parameter is not preserved

- **WHEN** a visitor is on `/photos?group=trips&debug=true` and the template invokes `${photoUrl.detail(photo)}`
- **THEN** the resulting URL contains `group=trips` but does NOT contain `debug=true`
