## Why

The theme currently exposes only `/photos` (list); there is no way for a visitor to open a single photo with adjacent photos for left/right navigation. Themes that want a "lightbox-like" experience must reverse-engineer the data flow themselves. This change adds a first-class detail page at `/photos/{name}` that preserves the originating list's filter/page/size context and exposes a thumbnail strip of neighboring photos, while standardizing how themes build URLs across both pages.

## What Changes

- Add a new theme route `GET /photos/{name}` that renders a `photo.html` template with the current photo plus a 5-item sliding window of neighboring photos.
- Detail route reads `group`, `page`, `size` from the query string, applies the same filter/sort as `/photos`, and computes neighbors within that filtered context.
- Compute "previous" and "next" by the existing canonical sort (`effectiveTime DESC, creationTimestamp DESC, metadata.name ASC`); window is fixed to 5 items and slides to keep its full width near the head/tail of the list.
- **BREAKING (with redirect)**: List pagination moves from path-style `GET /photos/page/{page}` to query-style `GET /photos?page={page}&size={size}`. Old path-style URLs return `301 Moved Permanently` to the new query form so existing inbound links keep working.
- Detail route handles error cases: nonexistent or soft-deleted photo → `404`; `group=` query parameter that does not match the photo's actual `spec.groupName` → `302` redirect to `/photos/{name}` with the bad `group` stripped.
- Add a `permalink` field to `PhotoVo`, populated by `PhotoVo.from(Photo)` to `/photos/{metadata.name}`. Available everywhere a `PhotoVo` is consumed (both via `PhotoFinder` and inside templates).
- Inject a per-request `photoUrl` helper bean into both list and detail handler models. The bean exposes `detail(photo)` / `detail(photo, overrides)` / `list()` / `list(group)` / `list(group, page, size)` so templates never need to hand-concatenate query strings or know which context parameters exist.
- Detail template model attributes: `photo`, `neighbors`, `prev`, `next`, `position`, `total`, `group`, `page`, `size`, `backUrl`, `title`, `_templateId="photo"`.

## Capabilities

### New Capabilities

- `photo-detail-page`: Theme-side routing contract for the photo gallery, covering both the list route, the new detail route, pagination URL scheme + redirect, neighbor-window computation, error handling, and the detail template's model contract.
- `theme-url-context`: Cross-page URL helpers — the `photoUrl` model bean and the `PhotoVo.permalink` field — that let theme templates build context-preserving links without manual query-string assembly.

### Modified Capabilities

None.

## Impact

- **Backend**: `PhotoRouter` gains a detail handler, a list handler refactor for query-style pagination, and a redirect mapping for the old path-style pagination URL. A new `PhotoUrlBuilder` class is added and registered into the model. `PhotoVo.from(Photo)` is updated to populate `permalink`.
- **Theme contract**: Themes need to ship a new `photo.html` template to render the detail page; without it, `/photos/{name}` falls through to Halo's default template-not-found behavior. Existing `photos.html` continues to work; theme authors are encouraged to migrate `<a>` links from manual concatenation to `${photo.permalink}` or `${photoUrl.detail(photo)}`.
- **Public API**: `PhotoFinder` is unchanged. `PhotoVo` gains one new field (`permalink`), which is additive and serializable.
- **Settings**: No new settings; window size is fixed at 5.
- **SEO**: Old `/photos/page/{page}` URLs now `301` to the new form, preserving link equity. Old links continue to resolve.
