## Context

`PhotoRouter` serves two Thymeleaf-rendered routes:

1. **List page** (`/photos`): Resolves `groups`, `photos`, and `title` from reactive sources (`photoFinder.groupBy()`, `photoPublicQueryService.listPhotos()`, `settingFetcher.getSettingValue()`) and places them into the model map before calling `ServerResponse.ok().render()`.
2. **Detail page** (`/photos/{name}`): Resolves `neighbors`, `prev`, `next`, `position`, `total`, and `title` from `photoPublicQueryService.listAllPhotos()` plus settings, and places them into the model map.

In both cases, the queries execute immediately because the model map is populated with `Mono`-derived values that are subscribed by the reactive pipeline before render time. If a theme template does not reference these variables (for example, a SPA theme that fetches data client-side from the public API), the queries still run, wasting resources.

Halo core addressed the same problem in PR #8006 by wrapping the `posts` variable for the index route in `LazyContextVariable`, leveraging Thymeleaf's built-in support for deferred variable resolution.

## Goals / Non-Goals

**Goals:**
- Eliminate unnecessary database queries when theme templates do not use `groups`, `photos`, `title`, `neighbors`, `prev`, `next`, `position`, or `total`.
- Keep the implementation backward-compatible; existing templates must work without modification.
- Stay consistent with the upstream Halo pattern (`LazyContextVariable` + `.block(timeout)`).

**Non-Goals:**
- Changing the Finder or Service APIs.
- Changing the public REST API (`api.photo.halo.run/v1alpha1`).
- Adding new template variables or removing existing ones.
- Migrating away from reactive types at the service layer.

## Decisions

### 1. Use `LazyContextVariable` with `.block(timeout)`

**Rationale**: Thymeleaf's `LazyContextVariable` only invokes `loadValue()` when the template expression engine accesses the variable. This is exactly what we need for on-demand loading. The `loadValue()` method is synchronous, so we must block the reactive pipeline to a synchronous value. Halo's `IndexRouteFactory` uses `ReactiveUtils.DEFAULT_TIMEOUT` (or a local `BLOCKING_TIMEOUT` constant) for this purpose.

**Alternative considered**: Passing `Mono` directly into the model map and relying on Thymeleaf-Spring integration to unwrap `Mono` lazily. This does not work because Thymeleaf-Spring's `ReactiveDataDriverContextVariable` is designed for streaming `Flux` data to `th:each`, not for lazy single-value resolution. `LazyContextVariable` is the correct tool.

### 2. Only wrap variables that trigger database or service calls

**Rationale**: Not all model entries need to be lazy. Synchronous, non-blocking values like `group`, `page`, `size`, `photo`, `photoUrl`, `backUrl`, and `ModelConst.TEMPLATE_ID` do not query the database and should remain as-is to avoid unnecessary complexity.

| Variable | Handler | Source | Wrap? |
|---|---|---|---|
| `groups` | `listHandler()` | `photoFinder.groupBy().collectList()` | Yes |
| `photos` | `listHandler()` | `photoPublicQueryService.listPhotos(...)` | Yes |
| `title` | `listHandler()` | `settingFetcher.getSettingValue(...)` | Yes |
| `photoUrl` | `listHandler()` | synchronous `new PhotoUrlBuilder(request)` | No |
| `photo` | `renderDetail()` | already resolved via `getByName()` | No |
| `neighbors` | `renderDetail()` | derived from `loadFilteredPhotos()` | Yes |
| `prev` | `renderDetail()` | derived from `loadFilteredPhotos()` | Yes |
| `next` | `renderDetail()` | derived from `loadFilteredPhotos()` | Yes |
| `position` | `renderDetail()` | derived from `loadFilteredPhotos()` | Yes |
| `total` | `renderDetail()` | derived from `loadFilteredPhotos()` | Yes |
| `title` | `renderDetail()` | `settingFetcher` or photo spec | Yes |
| `group` | `renderDetail()` | query param (sync) | No |
| `page` | `renderDetail()` | query param (sync) | No |
| `size` | `renderDetail()` | query param (sync) | No |
| `backUrl` | `renderDetail()` | synchronous string | No |
| `photoUrl` | `renderDetail()` | synchronous `new PhotoUrlBuilder(request)` | No |

### 3. Define `BLOCKING_TIMEOUT` as a static constant

**Rationale**: Consistent with `IndexRouteFactory` in Halo core. The timeout prevents a hung `block()` from holding a thread indefinitely if the reactive pipeline stalls. We should use a sensible default (e.g., `ReactiveUtils.DEFAULT_TIMEOUT` or `Duration.ofSeconds(5)`).

## Risks / Trade-offs

- **[Risk] Blocking inside `LazyContextVariable.loadValue()`** can consume a worker thread if the data source is slow.
  **Mitigation**: Use a reasonable timeout. The upstream Halo implementation uses the same pattern, and the underlying queries are fast indexed Extension lookups.

- **[Risk] Slight increase in render latency for templates that do use the variables** because the query now happens during Thymeleaf evaluation rather than during handler execution.
  **Mitigation**: This is negligible for typical use cases. The query still executes once; only the timing shifts. The benefit of skipping queries for unused variables outweighs this.

- **[Risk] Backward compatibility if a theme directly inspects the model map** (e.g., via a Thymeleaf `th:with` that checks the model object type).
  **Mitigation**: `LazyContextVariable` is transparent to Thymeleaf expressions. Normal variable access (`${groups}`, `${photos}`, etc.) works identically. This is not a breaking change for standard template usage.

## Migration Plan

No migration needed. This is a backward-compatible internal optimization. Themes and custom templates continue to work without changes.

## Open Questions

None.
