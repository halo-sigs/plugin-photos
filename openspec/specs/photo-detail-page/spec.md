## ADDED Requirements

### Requirement: Theme-Side Photo List Route With Query-String Pagination

The plugin SHALL register a theme-side `GET /photos` route that renders the gallery list. Pagination MUST use the query parameters `page` (1-based, default `1`) and `size` (default from `base.pageSize` setting). Filtering by group MUST use the query parameter `group`.

#### Scenario: Default list request

- **WHEN** a visitor requests `GET /photos` with no query parameters
- **THEN** the response renders the `photos` template with page 1 and the configured default page size, including all photos regardless of group

#### Scenario: List request with pagination parameters

- **WHEN** a visitor requests `GET /photos?page=3&size=20`
- **THEN** the response renders page 3 with page size 20, ordered by the canonical effective-time descending sort

#### Scenario: List request filtered by group

- **WHEN** a visitor requests `GET /photos?group=trips`
- **THEN** the response includes only photos whose `spec.groupName` equals `trips`, paginated using the default or supplied page parameters

### Requirement: Permanent Redirect From Legacy Path-Style Pagination

The plugin SHALL keep `GET /photos/page/{page}` resolvable by issuing a `301 Moved Permanently` response that points at `/photos?page={page}`. The redirect MUST NOT re-render the list template. The redirect target MUST NOT include a `size` parameter unless the original request supplied one (the legacy route never did).

#### Scenario: Legacy pagination URL

- **WHEN** a visitor requests `GET /photos/page/2`
- **THEN** the response is `301 Moved Permanently` with `Location: /photos?page=2`

#### Scenario: Legacy pagination URL preserves additional query parameters

- **WHEN** a visitor requests `GET /photos/page/2?group=trips`
- **THEN** the response is `301 Moved Permanently` with `Location: /photos?page=2&group=trips`

### Requirement: Theme-Side Photo Detail Route

The plugin SHALL register a theme-side `GET /photos/{name}` route that renders a single photo. The route MUST read context query parameters `group`, `page`, and `size` to mirror the originating list view.

#### Scenario: Detail request for an existing photo

- **WHEN** a visitor requests `GET /photos/{name}` and a `Photo` with that `metadata.name` exists and is not soft-deleted
- **THEN** the response renders the `photo` template with the photo and its neighboring photos as model attributes

#### Scenario: Detail request preserves list context

- **WHEN** a visitor requests `GET /photos/{name}?group=trips&page=2&size=20`
- **THEN** the rendered template receives `group=trips`, `page=2`, `size=20` as model attributes for use in back-links and neighbor links

### Requirement: Detail Route Returns 404 For Missing Or Deleted Photos

The detail route SHALL respond with `404 Not Found` when the requested photo does not exist or has a non-null `metadata.deletionTimestamp`.

#### Scenario: Photo name does not exist

- **WHEN** a visitor requests `GET /photos/{name}` and no Photo with that `metadata.name` exists
- **THEN** the response is `404 Not Found`

#### Scenario: Photo is soft-deleted

- **WHEN** a visitor requests `GET /photos/{name}` and the matching Photo has `metadata.deletionTimestamp` set
- **THEN** the response is `404 Not Found`

### Requirement: Detail Route Self-Corrects Mismatched Group Context

When the URL supplies a `group` query parameter that does not match the resolved photo's actual `spec.groupName`, the detail route SHALL respond with `302 Found` to the same `/photos/{name}` path with the `group` parameter removed and all other query parameters preserved.

#### Scenario: URL group does not match photo group

- **WHEN** a visitor requests `GET /photos/{name}?group=A&page=2` and the photo's `spec.groupName` is `B` (or empty)
- **THEN** the response is `302 Found` with `Location: /photos/{name}?page=2` (no `group` parameter)

#### Scenario: URL group matches photo group

- **WHEN** a visitor requests `GET /photos/{name}?group=A&page=2` and the photo's `spec.groupName` is `A`
- **THEN** the response renders the detail template (no redirect)

#### Scenario: URL has no group parameter

- **WHEN** a visitor requests `GET /photos/{name}` with no `group` parameter
- **THEN** the response renders the detail template using the global (unfiltered) list as context, regardless of the photo's own `spec.groupName`

### Requirement: Detail Route Computes A Five-Item Sliding Neighbor Window

The detail handler SHALL compute neighboring photos within the same filtered+sorted list that the originating `/photos` view would produce. The window MUST be fixed at 5 items and MUST slide near the head or tail to preserve its full width whenever the filtered list contains at least 5 photos.

The canonical sort MUST be the same effective-time comparator used by the list (`exif.dateTimeOriginal` descending falling back to `metadata.creationTimestamp` descending, with `metadata.creationTimestamp` descending and `metadata.name` ascending as tie-breakers).

#### Scenario: Current photo is in the middle of the list

- **WHEN** the filtered list has 20 photos and the current photo is at index 10 (0-based)
- **THEN** `neighbors` contains photos at indices 8, 9, 10, 11, 12 in sort order

#### Scenario: Current photo is at the head of the list

- **WHEN** the filtered list has 20 photos and the current photo is at index 0
- **THEN** `neighbors` contains photos at indices 0, 1, 2, 3, 4 in sort order

#### Scenario: Current photo is at the tail of the list

- **WHEN** the filtered list has 20 photos and the current photo is at index 19
- **THEN** `neighbors` contains photos at indices 15, 16, 17, 18, 19 in sort order

#### Scenario: Filtered list has fewer than five photos

- **WHEN** the filtered list has 3 photos
- **THEN** `neighbors` contains all 3 photos in sort order, regardless of which one is current

#### Scenario: Group context narrows the neighbor window

- **WHEN** the URL has `?group=trips` and the filtered list contains 10 photos in `trips`
- **THEN** the neighbor window is computed only within the 10 photos in `trips`, not within the global photo set

### Requirement: Detail Template Receives A Stable Set Of Model Attributes

The detail route SHALL expose the following model attributes to the `photo` template. Reactive data sources (`neighbors`, `prev`, `next`, `position`, `total`, `title`) SHALL be wrapped in `org.thymeleaf.context.LazyContextVariable` so they are resolved only when the template references them.

- `photo`: the current `PhotoVo`
- `neighbors`: the list of `PhotoVo` in the sliding window, in sort order, including the current photo — resolved lazily
- `prev`: the `PhotoVo` immediately before the current photo in sort order, or absent/null when the current photo is the first — resolved lazily
- `next`: the `PhotoVo` immediately after the current photo in sort order, or absent/null when the current photo is the last — resolved lazily
- `position`: the 1-based index of the current photo within the filtered context — resolved lazily
- `total`: the total count of photos in the filtered context — resolved lazily
- `group`: the `group` query parameter value (or null if absent)
- `page`: the `page` query parameter value (defaulting to 1)
- `size`: the `size` query parameter value (defaulting to the configured page size)
- `backUrl`: a prebuilt URL string back to the originating list view, including any active `group`, `page`, and `size`
- `title`: the page title string (from `base.title` setting or the photo's `spec.displayName`) — resolved lazily
- `_templateId`: the string `"photo"`
- `photoUrl`: the per-request URL helper described in the `theme-url-context` capability

#### Scenario: Single photo detail template rendering

- **WHEN** the detail route renders `photo.html` for a known photo
- **THEN** the template can access `photo`, `neighbors`, `prev`, `next`, `position`, `total`, `group`, `page`, `size`, `backUrl`, `title`, `_templateId`, and `photoUrl` from the rendering context, with lazy variables resolving transparently on first access

#### Scenario: First photo has no previous neighbor

- **WHEN** the current photo is at index 0 of the filtered list
- **THEN** `prev` is null/absent and `next` is the photo at index 1

#### Scenario: Last photo has no next neighbor

- **WHEN** the current photo is at the last index of the filtered list
- **THEN** `next` is null/absent and `prev` is the photo immediately before

#### Scenario: SPA theme does not trigger neighbor queries

- **WHEN** the `photo.html` template does not reference `${neighbors}`, `${prev}`, `${next}`, `${position}`, `${total}`, or `${title}`
- **THEN** the underlying filtered photo list query does not execute

### Requirement: Detail Route Does Not Pre-Check Template Existence

The detail route SHALL NOT inspect whether the active theme provides `photo.html`. When the template is missing, the response falls through to Halo's default template-not-found handling.

#### Scenario: Theme without photo.html

- **WHEN** the active theme has no `photo.html` template and a visitor requests a valid `/photos/{name}` URL
- **THEN** the plugin returns the model normally and Halo's default template resolution determines the user-facing response (no plugin-side 404 or redirect)
