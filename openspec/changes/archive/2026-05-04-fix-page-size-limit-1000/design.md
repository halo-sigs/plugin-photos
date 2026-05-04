## Context

The plugin uses `PageRequestImpl.of(1, Integer.MAX_VALUE, …)` in several locations to request "all" data in a single page. Halo’s `PageRequestImpl` caps page size at `MAX_SIZE = 1_000` and logs a warning when exceeded. While some current code paths happen to bypass the cap via `ReactiveExtensionClient.listAll`, the pattern is brittle:

- Any future change to sort criteria can silently shift a query from `listAll` to `listBy`, causing truncation.
- The warning logs create operational noise.
- Loading unbounded data into memory via `listAll` does not scale for very large galleries.

The affected call sites are:
- `PhotoFinderImpl.listAll()` — line 37
- `PhotoFinderImpl.listBy(group)` — line 62
- `PhotoFinderImpl.groupBy()` — lines 69 and 77
- `PhotoRouter.loadFilteredPhotos()` — line 188

## Goals / Non-Goals

**Goals:**
- Replace every `Integer.MAX_VALUE` page request with explicit, safe paged iteration.
- Ensure `photoFinder.listAll()`, `photoFinder.listBy(group)`, `photoFinder.groupBy()`, and `PhotoRouter` SSR neighbours always return complete results regardless of gallery size.
- Eliminate `Page size must not be greater than 1000` warnings from the plugin.

**Non-Goals:**
- Changing any API signatures or SPI contracts (`PhotoFinder`, `PhotoPublicQueryService`).
- Altering sort order, pagination defaults, or template model attributes.
- Introducing a generic reusable pagination helper outside the plugin (keep the fix local).

## Decisions

### 1. Add `listAllPhotos` / `listAllGroups` to `PhotoPublicQueryService`

Rather than building ad-hoc pagination loops inside `PhotoFinderImpl` and `PhotoRouter`, the iteration logic belongs in `PhotoPublicQueryService` — the declared single read-path for public data.

- **Rationale**: Keeps the fix in one place, respects the existing architecture, and makes it impossible for future callers to accidentally use the oversized-page antipattern.
- **Alternative considered**: A private `fetchAllPages` helper in `PhotoFinderImpl` only. Rejected because `PhotoRouter` also needs the same logic for `loadFilteredPhotos`, and duplicating it would violate the single-read-path design.

### 2. Use `expand` for page-by-page reactive iteration

Implementation uses Reactor’s `expand` operator:

```java
fetchPage(1)
    .expand(result -> {
        long totalPages = ceil((double) result.getTotal() / result.getSize());
        return result.getPage() < totalPages
            ? fetchPage(result.getPage() + 1)
            : Mono.empty();
    })
    .concatMapIterable(ListResult::getItems);
```

- **Rationale**: `expand` is back-pressure-aware and idiomatic for recursively fetching linked pages. `concatMapIterable` preserves ordering and does not interleave pages.
- **Alternative considered**: `Flux.range(1, totalPages).concatMap(page -> …)`. Rejected because it requires either a pre-flight count request or computing total pages from the first result then re-emitting, which is less clean than `expand`.

### 3. Page size for iteration: 500

The internal chunk size for all-by-all iteration is set to 500.

- **Rationale**: Well under Halo’s 1_000 cap, large enough to avoid excessive round-trips for moderate galleries, small enough to keep individual response payloads bounded.
- **Alternative considered**: 1_000. Rejected because landing exactly on the cap is risky if Halo ever tightens the limit.

### 4. Keep `listPhotos` / `listGroups` unchanged for normal paginated callers

`PhotoPublicQueryService.listPhotos(PageRequest)` and `listGroups(PageRequest)` continue to serve regular paginated requests (themes, public API) exactly as before. The new `listAllPhotos` / `listAllGroups` are additional methods for callers that truly need the full stream.

- **Rationale**: Avoids breaking existing consumers or changing observable latency for ordinary paginated requests.

## Risks / Trade-offs

- **[Risk]** `expand` + `concatMapIterable` emits one page at a time; for very large galleries (>10 000 photos) the sequential fetch may be slower than a single `listAll`.
  → **Mitigation**: 500-item pages mean at most 20 requests for a 10 000-photo gallery, which is acceptable. If performance becomes a concern later, a parallel `flatMapSequential` variant can be introduced behind a size threshold.

- **[Risk]** `PhotoFinderImpl.groupBy()` currently uses `client.listAll` for groups and then fetches photos per group. After the change, each group triggers its own paged iteration. A gallery with many groups could generate more queries.
  → **Mitigation**: The number of groups is typically small (tens, not thousands). The change makes the query correct rather than silently truncated.

- **[Risk]** `PhotoRouter` detail-page neighbour computation (`loadFilteredPhotos`) now iterates pages instead of a single `listAll` call.
  → **Mitigation**: This is the same risk as above; correctness trumps the marginal extra round-trips.

## Migration Plan

No migration needed. This is a transparent bug fix with no database, API, or template changes.

## Open Questions

(none)
