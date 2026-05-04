## Context

The plugin currently exposes a single theme route, `GET /photos`, which renders the gallery list with optional `group` filtering and path-style pagination at `GET /photos/page/{page}`. There is no detail view; if a theme wants to open a single photo with prev/next navigation, the theme author must reverse-engineer the data flow themselves.

`PhotoRouter` builds a `RouterFunction` that delegates list paging to `PhotoFinder.list(page, size, group)`, which in turn loads all matching photos via `ReactiveExtensionClient.listAll`, sorts them with `PhotoSortUtils.effectiveTimeComparator`, and slices the page in memory. This in-memory pattern is the natural foundation for computing neighbors, since the same filter+sort context that produced the list is what we need to walk to find adjacent photos.

`PhotoVo` is an immutable Lombok `@Value @Builder` exposed by both `PhotoFinder` and the router; any URL we expose on it must be derivable from the data it already holds. The existing settings (`base.title`, `base.pageSize`) and the canonical sort key are stable enough that hardcoding URL formats in one place is acceptable.

## Goals / Non-Goals

**Goals:**

- Provide a theme-side detail route at `/photos/{name}` that preserves the originating list's filter context (group, page, size).
- Expose a 5-item sliding window of neighboring photos for use as a thumbnail strip, matching what the visitor would see in the originating list.
- Standardize URL construction across list and detail pages so theme authors do not concatenate query strings by hand and so future filter parameters (tags, keywords) compose automatically.
- Migrate list pagination to a query-string convention while keeping old links working through a 301 redirect.

**Non-Goals:**

- Expose new finder methods. Detail/neighbor concerns are presentation-only; finder API surface stays as-is.
- Add settings for window size or detail-page toggling. Window is a fixed 5; absence of `photo.html` in a theme is the theme author's responsibility.
- Change the canonical sort. `effectiveTime DESC, creationTimestamp DESC, metadata.name ASC` is reused exactly.
- Provide SEO/canonical/og metadata, sitemap entries, or share-image generation.
- Add a permalink for `PhotoGroup` (group permalinks are query-based, not path-based; they have a different shape and are not in scope here).
- Add console-side UI for the detail page.

## Decisions

### Decision 1: Path-style detail URL `/photos/{name}` and query-style list pagination

Use `GET /photos/{name}` for the detail route and migrate list pagination from `GET /photos/page/{page}` to `GET /photos?page={page}&size={size}`. Old path-style URLs return `301 Moved Permanently` to the new query form.

Rationale: with pagination moved off the path, `/photos` carries only two semantics — list (no path variable) and detail (single name segment) — so there is no router ambiguity. Path-style detail URLs read more naturally than `/photos/view?name=`, match Halo's broader convention (`/archives/{slug}`, `/tags/{name}`), and let `metadata.name` act as a stable identifier without query-string ceremony.

Alternative considered: keep `/photos/page/{page}` and use `/photos/view?name=` for detail. Rejected because it forces every list link to know about both pagination styles and locks `name` into a query parameter, making the canonical permalink uglier.

### Decision 2: Compute neighbors from the same filtered+sorted list as the list page

The detail handler reads `group`, `page`, `size` from the request, applies the same `ListOptions` filter and `effectiveTimeComparator` sort that `PhotoFinder.list(page, size, group)` would use, locates the current photo by `metadata.name` in that ordered list, and slices a sliding window of 5 around it.

Rationale: this guarantees that prev/next/neighbors match exactly what the visitor saw in the originating list. The same in-memory load-and-sort pattern is already used by `pagePhoto`, so memory and CPU costs are proportional to the gallery size and consistent with what the list page already pays. No new data path is introduced.

Alternative considered: index-based neighbor lookup using a sorted database query with `ORDER BY ... LIMIT 1`. Rejected because the canonical sort key (`effectiveTime`) is a coalesced value (`spec.dateTimeOriginal` falling back to `metadata.creationTimestamp`) that the index cannot express directly, so we would need a denormalized field plus migration. Out of proportion with the change.

### Decision 3: Sliding window of fixed size 5

When the current photo is not near the head or tail, `neighbors` is `[c-2, c-1, c, c+1, c+2]`. Near the boundaries, the window slides to keep its full width: at index 0, `[0, 1, 2, 3, 4]`; at the last index, `[n-5, n-4, n-3, n-2, n-1]`.

Rationale: predictable, full-width thumbnail strips match common gallery viewers (Apple Photos, Lightroom). Truncating at the edges makes the strip width vary, which reads as visual instability.

Edge case: if the total filtered list has fewer than 5 photos, `neighbors` contains all of them. If it has exactly 5, the window is the entire list.

Alternative considered: configurable window size via plugin settings. Deferred — fixed 5 covers expected use; the constant lives in one place, so a future change can promote it to a setting if real demand appears.

### Decision 4: Group mismatch returns `302` with `group` stripped

If the URL has `?group=A` but the resolved photo's actual `spec.groupName` is not `A`, the handler returns `302 Found` to `/photos/{name}` with all other context preserved except `group`. The redirect target has no `group` parameter, so the new request cannot trigger the same mismatch — no redirect loop is possible.

Rationale: the visitor's deep link is honored (they still see the right photo), the URL self-corrects, and the neighbor strip on the redirected page is still meaningful (it falls back to the global filtered set, which is what "no group filter" already means on the list page). Returning 404 would punish a benign URL edit; silently ignoring `group` would leave a misleading parameter in the address bar.

Alternative considered: redirect to the photo's actual group (`/photos/{name}?group={photo.spec.groupName}`). Rejected because it second-guesses the visitor — the URL edit may have been intentional (e.g., navigating between groups) and the photo's own group is not necessarily where the visitor wanted to land.

### Decision 5: `PhotoVo.permalink` populated by the factory

Add `String permalink` to `PhotoVo` and set it inside `PhotoVo.from(Photo)` to `"/photos/" + photo.getMetadata().getName()`. No setter, no contextual variants.

Rationale: matches the Halo convention of `${post.permalink}` — a stable, context-free URL is part of the data shape that represents a publishable resource. Putting it in the factory means every consumer (finder calls, router, future endpoints) gets it automatically with no plumbing. `metadata.name` in Halo is always non-null and URL-safe, so no encoding or null-handling is needed.

Alternative considered: omit `permalink` from `PhotoVo` and rely entirely on the `photoUrl` helper. Rejected because non-template consumers of the finder (RSS feeds, sitemap generators, custom endpoints in other plugins) cannot access the model bean and would have to hardcode the URL pattern themselves.

### Decision 6: `photoUrl` helper bean is per-request and not exposed via `PhotoFinder`

Construct a `PhotoUrlBuilder` instance inside each handler with the current `ServerRequest`, attach it to the model under the key `photoUrl`. The bean has no static state and is not registered as a Spring bean.

Rationale: URL building is a presentation concern that needs the live request to preserve query parameters; finders return raw data and run outside any specific request context. Keeping the helper out of `PhotoFinder` preserves that boundary and avoids exposing a context-coupled API to finder callers in other plugins.

Method shape:
- `detail(PhotoVo photo)` — `/photos/{name}` plus the current request's whitelist of context query parameters
- `detail(PhotoVo photo, Map<String, ?> overrides)` — same, with caller overrides applied last
- `list()` — bare `/photos`
- `list(String group)` — `/photos?group={group}` (omits when blank)
- `list(String group, int page, int size)` — full list URL with pagination

The whitelist of context parameters is currently `{group, page, size}`. When new filter parameters are introduced, the whitelist is extended in one place and all themes pick it up automatically.

### Decision 7: Pagination URL migration uses 301, not silent removal

`GET /photos/page/{page}` is preserved as a thin redirect handler that issues `301 Moved Permanently` to `/photos?page={page}` (no `size` parameter, since the old route had no size variable). The handler does not re-render anything.

Rationale: `301` keeps inbound links and search-engine link equity working, and `Moved Permanently` tells crawlers to update their indexes. A silent removal would 404 every existing link.

Alternative considered: keep both routes live indefinitely. Rejected because dual routes confuse theme authors about which to link to and bloat the route table; a clean redirect leaves only one canonical path.

### Decision 8: Template missing is not handled by the plugin

If a theme does not ship `photo.html`, the plugin returns the model and lets Halo's standard template-not-found behavior take over. The plugin does not pre-check template existence or short-circuit to 404.

Rationale: this matches how Halo posts behave when a theme lacks `post.html`, keeps the plugin code free of theme-introspection, and lets theme authors decide how to surface the missing template. The plugin will document the new template in its README so theme authors know to add it.

## Risks / Trade-offs

- **Memory cost of neighbor computation on huge galleries** → the detail handler loads the entire filtered list into memory (same as the list page already does). Mitigation: this matches the existing list-page cost profile; if it becomes a bottleneck, a future change can introduce a denormalized sort field plus indexed neighbor queries. No new ceiling is being introduced.
- **`group` redirect loop possibility** → mitigated structurally: the redirect target strips the `group` parameter, so the new request cannot trigger the same mismatch handler again.
- **Theme upgrade gap** → existing themes will not have `photo.html`. Mitigation: documented as a theme author task; without the template, list pages continue working unchanged and detail URLs simply fail to render. No silent regression.
- **Hardcoded URL format in two places** → `PhotoVo.from(Photo)` hardcodes `/photos/{name}`, and `PhotoUrlBuilder` constructs the same pattern. Mitigation: extract a small package-private constant if a third place ever needs it; until then, two call sites is acceptable duplication.
- **Stale link equity from old pagination URLs** → `301` redirects preserve the equity and signal the move to crawlers; the only loss is deep links that bookmarked specific high-numbered pages, which are rare in galleries.

## Migration Plan

No data migration is required. The change is read-only on the data path and only adds routes and one VO field.

Deployment steps (per release):
1. Build the plugin and ship it. Themes that already have `photos.html` continue rendering it; the new `photoUrl` bean and `permalink` field are simply unused.
2. Theme authors who want the detail page add `photo.html` to their theme and optionally migrate `photos.html` `<a>` links from manual concatenation to `${photoUrl.detail(photo)}` or `${photo.permalink}`.
3. Old `/photos/page/{page}` links keep resolving via the 301 redirect.

Rollback: revert the plugin version. The 301 mapping disappears, but the original path-style pagination route returns, so there is no link breakage in either direction.

## Open Questions

None at this stage. Future considerations parked for follow-up changes:
- When user-configurable sort options reach the URL, the detail route's neighbor computation must include the sort key in its context whitelist.
- Tag and keyword filters, when added, will need to be added to the `photoUrl` whitelist and to the detail handler's filter logic; the helper-bean pattern was chosen specifically so this extension is a one-place edit.
- SEO concerns (canonical, og:image, share metadata) will be addressed when a theme-author-facing audit is requested.
