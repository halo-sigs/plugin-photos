## Why

`PhotoPublicQueryServiceImpl.listTags()` and `PhotoServiceImpl.listAllTags()` both call `client.listAll(Photo.class, ...)` with no filter, deserializing every Photo object in the store just to extract tag strings. As the photo count grows, this causes unnecessary CPU and GC pressure on every tag-list request. A `spec.tags` multi-index already exists but is not exploited to narrow the scan.

## What Changes

- Add `isNotNull("spec.tags")` filter to `listAllTags()` in `PhotoServiceImpl` so only tagged photos are deserialized.
- Add `isNotNull("spec.tags")` filter to `listTags()` in `PhotoPublicQueryServiceImpl` for the same reason.
- Introduce a short-lived in-memory cache (Caffeine, TTL ~2 min) in `PhotoPublicQueryServiceImpl.listTags()` so repeated theme renders skip the full scan on a cache hit.
- Add Caffeine as a runtime dependency (already on classpath via Spring Boot if present, otherwise add explicitly).

## Capabilities

### New Capabilities

_(none — no new user-visible capabilities are introduced)_

### Modified Capabilities

- `public-api`: The `/tags` endpoint implementation changes internally (cache + narrower scan). Observable behavior — response content and shape — is unchanged; only latency and resource usage improve.

## Impact

- **Backend files**: `PhotoServiceImpl.java`, `PhotoPublicQueryServiceImpl.java`, `build.gradle` (Caffeine dependency if needed)
- **No API contract changes**: request/response shape for `/tags` and the console tag endpoint is identical
- **No frontend changes required**
