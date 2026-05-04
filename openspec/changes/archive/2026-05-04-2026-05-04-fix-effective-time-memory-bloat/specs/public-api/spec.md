## ADDED Requirements

### Requirement: Public Photo List Effective-Time Sort Is Index-Backed

`GET /apis/api.photo.halo.run/v1alpha1/photos` for the default ordering and for `sort=exif.dateTimeOriginal,(asc|desc)` SHALL be served by an index-backed query whose memory and CPU cost per request is bounded by the requested page size, not by the total number of non-deleted photos in the gallery. The implementation SHALL NOT load every matching photo into memory in order to apply the EXIF-time-then-creation-time fallback.

The same guarantee SHALL apply to data flowing through `PhotoPublicQueryService.listPhotos(...)` regardless of whether the caller is the public endpoint, the `PhotoFinder` SPI, or the SSR `PhotoRouter`.

#### Scenario: Public default ordering on a large gallery is page-bounded

- **WHEN** an anonymous client issues `GET /apis/api.photo.halo.run/v1alpha1/photos?page=1&size=20` (no `sort` parameter) against a gallery of 50 000 photos
- **THEN** the server responds without first loading all 50 000 photos into memory for sorting
- **THEN** the page-1 response contains 20 items ordered by effective time (`exif.dateTimeOriginal` when present, otherwise `metadata.creationTimestamp`), then `metadata.creationTimestamp desc`, then `metadata.name asc`
- **THEN** repeated requests at the same page exhibit response time and heap allocation bounded by page size, not by gallery size

#### Scenario: Finder paged iteration does not multiply table loads

- **WHEN** a Thymeleaf template calls `photoFinder.listAll()` against a gallery of 50 000 photos and `PhotoPublicQueryService.listAllPhotos(...)` iterates through pages of 500
- **THEN** each page fetch is served by an indexed query whose cost is bounded by the page size of 500, not by the full gallery size
- **THEN** total work for the iteration is O(N) over the gallery, not O(N^2)

#### Scenario: SSR neighbour computation is page-bounded per request

- **WHEN** an anonymous visitor requests `GET /photos/{name}` and `PhotoRouter` computes neighbour photos via `PhotoPublicQueryService.listAllPhotos(...)`
- **THEN** each underlying page fetch is served by an indexed query whose cost is bounded by page size
- **THEN** the rendered model still contains the same `neighbors`, `prev`, `next`, `position`, `total` attributes as before this change
