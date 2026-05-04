## 1. PhotoVo Permalink Field

- [x] 1.1 Add a `String permalink` field to `PhotoVo` (`src/main/java/run/halo/photos/vo/PhotoVo.java`).
- [x] 1.2 Update `PhotoVo.from(Photo)` to populate `permalink` with `"/photos/" + photo.getMetadata().getName()`.
- [x] 1.3 Confirm via grep that all `PhotoVo` construction goes through `PhotoVo.from(Photo)`; if any other builder usage exists, ensure `permalink` is set there too.

## 2. PhotoUrlBuilder Helper

- [x] 2.1 Create `PhotoUrlBuilder` (e.g. `src/main/java/run/halo/photos/PhotoUrlBuilder.java`) that holds a `ServerRequest` (or its query-parameter map) and exposes `detail(PhotoVo)`, `detail(PhotoVo, Map<String, ?> overrides)`, `list()`, `list(String group)`, and `list(String group, int page, int size)`.
- [x] 2.2 Define the context-parameter whitelist (`group`, `page`, `size`) as a private constant inside the helper so future filters extend in one place.
- [x] 2.3 Ensure URL construction uses `UriComponentsBuilder` (matching the existing `appendGroupParam` pattern), drops blank/non-positive parameters, and applies `overrides` last so caller intent wins.
- [x] 2.4 Make sure `detail(PhotoVo)` does not propagate query parameters outside the whitelist (e.g., a `?debug=true` on the request should not appear in the produced URL).

## 3. List Route Migration To Query-String Pagination

- [x] 3.1 In `PhotoRouter`, refactor the list handler so it reads `page` (default 1) and `size` (default from `base.pageSize` setting) from query parameters instead of the `{page}` path variable.
- [x] 3.2 Inject a per-request `PhotoUrlBuilder` into the list-page model under the key `photoUrl`.
- [x] 3.3 Update internal next/prev URL construction (currently using `PageUrlUtils`) so the pagination links emitted to the template use `?page=&size=&group=` form, not `/page/{n}`.

## 4. 301 Redirect For Legacy Pagination URLs

- [x] 4.1 Register a separate route `GET /photos/page/{page:\\d+}` whose handler returns `301 Moved Permanently` with `Location` pointing at `/photos?page={page}` plus any preserved query parameters from the original request (notably `group`).
- [x] 4.2 Verify the redirect handler does not invoke the list rendering path (it should only build the redirect URL and return the response).
- [x] 4.3 Confirm route ordering in `photoRouter()` so the legacy redirect route does not shadow the new `/photos/{name}` detail route or vice-versa.

## 5. Detail Route Handler

- [x] 5.1 Register `GET /photos/{name}` in `photoRouter()` and add a corresponding handler in `PhotoRouter`.
- [x] 5.2 Implement photo lookup by `metadata.name` via `ReactiveExtensionClient.fetch(Photo.class, name)`. Treat absent or `metadata.deletionTimestamp != null` as 404.
- [x] 5.3 Read context query parameters (`group`, `page`, `size`) from the request and prepare the same `ListOptions` filter the list handler uses.
- [x] 5.4 Compare URL `group` with photo `spec.groupName`; on mismatch return `302 Found` to `/photos/{name}` with `group` removed and other query parameters preserved.
- [x] 5.5 Build the filtered+sorted list using the same load-and-sort pattern as `PhotoFinderImpl.pagePhoto` (`client.listAll(...)` with `equal("spec.groupName", group)` when present, then `PhotoSortUtils.effectiveTimeComparator(false)`).
- [x] 5.6 Locate the current photo's index in the sorted list and compute the 5-item sliding neighbor window (clamp to `[0, total-5]`, fall back to the entire list when `total < 5`).
- [x] 5.7 Compute `prev` (or null when current is index 0) and `next` (or null when current is the last index).
- [x] 5.8 Render the `photo` template with model attributes: `photo`, `neighbors`, `prev`, `next`, `position` (1-based), `total`, `group`, `page`, `size`, `backUrl` (built via the URL helper's `list(group, page, size)`), `title`, `_templateId="photo"`, `photoUrl`.

## 6. Reference Theme Template

- [x] 6.1 Add `photo.html` under `workplace/themes/theme-earth/templates/` mirroring the existing `photos.html` styling and demonstrating: main image, prev/next links using `${photoUrl.detail(prev|next)}`, neighbor thumbnail strip, and a back-to-list link using `${backUrl}`.
- [x] 6.2 Update `workplace/themes/theme-earth/templates/photos.html` so each thumbnail wraps its `<img>` in `<a th:href="${photoUrl.detail(photo)}">` (or `${photo.permalink}`) so visitors can navigate to the new detail page.

## 7. Documentation

- [x] 7.1 Update the plugin README (or a theme-developer notes section if one exists) to document: the new `/photos/{name}` route, the model attributes available to `photo.html`, the recommendation to use `${photoUrl.detail(...)}` and `${photo.permalink}` instead of manual URL concatenation, and the migration of `/photos/page/{page}` to `?page=`.
- [x] 7.2 Note in the documentation that themes without `photo.html` will fall back to Halo's default template-not-found behavior.

## 8. Verification

- [x] 8.1 Add backend unit tests for `PhotoUrlBuilder` covering: detail URL with full context, detail URL with blank group dropped, detail URL with overrides, detail URL ignoring non-whitelisted parameters, list URL variants (no args, group only, full pagination).
- [x] 8.2 Add backend tests covering neighbor window computation: middle index, head index, tail index, list shorter than 5 photos, list of exactly 5 photos.
- [x] 8.3 Add backend tests covering detail-route error paths: missing photo → 404, soft-deleted photo → 404, group mismatch → 302 with stripped `group`, group match → render.
- [x] 8.4 Add backend tests covering the 301 redirect from `/photos/page/{page}` to `/photos?page={page}` with and without an additional `group` query parameter.
- [x] 8.5 Run `./gradlew test`.
- [x] 8.6 Run `./gradlew build` to confirm the plugin compiles cleanly with the new VO field, helper, and routes.
- [x] 8.7 Smoke-test the end-to-end flow against `./gradlew haloServer` using the reference theme: load `/photos`, click a thumbnail, confirm detail page renders, prev/next navigation works, back link returns to the same list page with group/page/size preserved, and the legacy `/photos/page/2` URL redirects with `301`.
  - ✅ `/photos` list page renders with group tabs and paginated thumbnails
  - ✅ Thumbnail links carry `group`/`page`/`size` context into detail URL
  - ✅ Detail page (`photo.html`) renders with title, image, position counter
  - ✅ Prev/next navigation preserves context params
  - ✅ 5-item neighbor thumbnail strip slides correctly
  - ✅ Back link returns to same list page with `group`/`page`/`size` preserved
  - ✅ Legacy `/photos/page/2` → `301 Moved Permanently` → `/photos?page=2`
