## 1. Backend Sorting Semantics

- [x] 1.1 Add or update shared backend sorting logic that computes effective photo time from `spec.dateTimeOriginal` with fallback to `metadata.creationTimestamp`.
- [x] 1.2 Support both ascending and descending effective-time comparators with deterministic tie-breakers including `metadata.creationTimestamp` and `metadata.name`.
- [x] 1.3 Ensure null handling is safe when EXIF time or creation timestamp is absent.

## 2. Console API and Finder Integration

- [x] 2.1 Update console photo listing so the default unsorted request uses effective photo time descending.
- [x] 2.2 Update `sort=spec.dateTimeOriginal,desc` and `sort=spec.dateTimeOriginal,asc` to use effective photo time ordering instead of placing EXIF-empty photos at the end.
- [x] 2.3 Keep `sort=metadata.creationTimestamp,desc` and `sort=metadata.creationTimestamp,asc` behavior unchanged.
- [x] 2.4 Update theme finder default ordering for `listAll()`, `list(page, size)`, `listBy(groupName)`, and grouped photo lists to use effective photo time descending.
- [x] 2.5 Confirm no frontend UI or generated API client changes are required because query parameter names and values stay unchanged.

## 3. Verification

- [x] 3.1 Add or update backend tests covering mixed photos with and without `spec.dateTimeOriginal` for descending and ascending shooting-time sorts.
- [x] 3.2 Add or update tests for creation-time sorting to verify it remains unchanged.
- [x] 3.3 Add or update finder tests covering default effective-time descending order across all-photo and grouped-list paths.
- [x] 3.4 Run `./gradlew test`.
- [x] 3.5 Run `./gradlew build` if implementation touches generated OpenAPI output or frontend-facing integration.
