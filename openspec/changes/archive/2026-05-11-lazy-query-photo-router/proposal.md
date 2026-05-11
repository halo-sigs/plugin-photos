## Why

`PhotoRouter` currently eagerly queries `groups`, `photos`, `title`, and other model data for every request to `/photos`, even when the Thymeleaf template does not reference those variables. This causes unnecessary database queries and Finder calls when a theme renders the page entirely via the public API (e.g., `api.photo.halo.run/v1alpha1/photos`). Following Halo's upstream improvement (halo-dev/halo#8006), we should load template variables lazily so data is only fetched when the template actually accesses it.

## What Changes

- Wrap reactive data lookups in `PhotoRouter.listHandler()` (`groups`, `photos`, `title`) with `org.thymeleaf.context.LazyContextVariable`, loading via `.block(timeout)` only when Thymeleaf evaluates the variable.
- Wrap reactive data lookups in `PhotoRouter.renderDetail()` (`neighbors`, `prev`, `next`, `position`, `total`, `title`) with `LazyContextVariable` in the same way.
- Add a static `BLOCKING_TIMEOUT` constant (aligned with `ReactiveUtils.DEFAULT_TIMEOUT` or a sensible default) for the `.block()` calls inside `LazyContextVariable.loadValue()`.
- Synchronous model entries (`group`, `page`, `size`, `photo`, `photoUrl`, `backUrl`, `ModelConst.TEMPLATE_ID`) remain unchanged.

## Capabilities

### New Capabilities

- `lazy-theme-model-loading`: Template model variables in `/photos` and `/photos/{name}` routes are loaded on-demand using Thymeleaf's `LazyContextVariable`, reducing unnecessary database queries when themes do not reference those variables.

### Modified Capabilities

- `theme-photo-routing`: Behavior change — `listHandler()` and `renderDetail()` model maps now contain `LazyContextVariable` wrappers instead of eagerly resolved `Mono` values. Existing templates that reference the variables will continue to work unchanged; the data is simply fetched lazily at variable access time.

## Impact

- **Backend**: `PhotoRouter.java` only.
- **APIs**: No API contract changes; this is an internal rendering optimization.
- **Themes**: Fully backward-compatible. Themes that use the variables continue to work; themes that don't use them benefit from fewer queries.
- **Dependencies**: Requires `thymeleaf` (already present as a transitive dependency through Halo's theme engine).
