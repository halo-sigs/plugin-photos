## Context

Both `PhotoPublicQueryServiceImpl.listTags()` and `PhotoServiceImpl.listAllTags()` call `client.listAll(Photo.class, empty options)`, deserializing every Photo object in the in-memory store to extract `spec.tags`. The `spec.tags` multi-index already exists (`PhotoPlugin.java`), but the current queries provide no filter predicate, so the index cannot prune the result set before deserialization.

The Halo framework's `ReactiveExtensionClient` does not expose an API to enumerate distinct index key values directly, so we cannot bypass Photo deserialization entirely. The best we can do within the current framework is:
1. Reduce the deserialization work via an index-backed `isNotNull` filter.
2. Cache results to avoid repeated scans on frequently-called endpoints.

## Goals / Non-Goals

**Goals:**
- Reduce the number of Photo objects deserialized on each `listTags` call by filtering to only tagged photos using the existing `spec.tags` index.
- Eliminate redundant scans via a short-lived in-memory cache on the public `listTags` endpoint.
- Zero behavioral change to callers (identical response shape and semantics).

**Non-Goals:**
- Introducing a new `Tag` Extension resource or reconciler.
- Persistent caching (disk/Redis).
- Exact real-time photoCount accuracy — a ~2-minute staleness window on the cache is acceptable.
- Changing `Photo.spec.tags` data model.

## Decisions

### 1. Use `isNotNull("spec.tags")` filter

`spec.tags` is registered as a `multi` index in `PhotoPlugin`. The `IsNotNullCondition` query class is available in the framework. Applying `isNotNull("spec.tags")` as a `ListOptions` query means the index layer returns only photo names that have at least one tag entry, avoiding deserialization of untagged photos entirely.

Applied to both:
- `PhotoServiceImpl.listAllTags(PhotoQuery)` — console API
- `PhotoPublicQueryServiceImpl.listTags(String nameFilter)` — public/theme API

### 2. Caffeine TTL cache in `PhotoPublicQueryServiceImpl.listTags()`

The public `listTags()` is called on every theme page render. Caffeine is already on the classpath via the Spring Boot platform BOM (no new `build.gradle` dependency needed).

Cache design:
- **Key**: `nameFilter` string (empty string for "all tags")
- **Value**: `List<PhotoTagVo>` — fully computed result
- **TTL**: 2 minutes (`expireAfterWrite`)
- **Max size**: 100 entries (nameFilter values are short strings, values are small lists)
- **Invalidation**: TTL only (no explicit invalidation on write — staleness window is acceptable for a tag cloud)

Reactive integration: check cache synchronously before starting the reactive pipeline; populate cache in `doOnNext` after collecting to list.

The console `listAllTags()` is a management API called interactively (not on every page render), so the isNotNull filter alone is sufficient there — no cache needed.

### 3. No new `build.gradle` dependency

Caffeine (`com.github.ben-manes.caffeine:caffeine`) is provided by the Spring Boot BOM already resolved by the Halo platform. It can be used directly without declaring an additional dependency.

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| `isNotNull` evaluated post-deserialization (framework detail) | If so, the filter still reduces stream work even if it doesn't reduce deserialization; worst case is a no-op optimization |
| Cache returns stale counts after rapid photo-tag updates | Acceptable for a tag cloud; TTL of 2 min is a reasonable staleness window |
| Caffeine not on runtime classpath in all Halo deployments | Unlikely given Spring Boot BOM; if it fails, fall back to no-cache approach |
