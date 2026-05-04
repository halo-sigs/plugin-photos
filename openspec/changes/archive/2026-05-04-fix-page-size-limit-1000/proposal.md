## Why

Halo's `PageRequestImpl` silently caps page size at 1000. The plugin currently uses `PageRequestImpl.of(1, Integer.MAX_VALUE, ...)` in `PhotoFinderImpl` and `PhotoRouter` to fetch "all" data in a single call, but anything above 1000 is reset to 1000. This causes incomplete results when a gallery exceeds 1000 photos — `listAll()`, `listBy(group)`, and `groupBy()` all silently truncate data.

## What Changes

- Fix `PhotoFinderImpl` to iterate through all pages using reactive expansion (`expand`) instead of requesting everything in one oversized page.
- Fix `PhotoRouter` SSR data loading to use the same paged-iteration pattern so theme-side model attributes contain the full dataset regardless of photo count.
- No API or SPI signatures change; this is a transparent bug fix.

## Capabilities

### New Capabilities
<!-- None — this is a pure implementation fix. -->
(none)

### Modified Capabilities
<!-- No spec-level requirement changes; existing specs already demand full results. This change only corrects the implementation to satisfy them. -->
(none)

## Impact

- `PhotoFinderImpl.java` — `listAll()`, `list(page,size)`, `listBy(group)`, `groupBy()`
- `PhotoRouter.java` — photo list / group list model population
- Behavior change: galleries with >1000 photos now return complete results instead of silently truncating to the first 1000
