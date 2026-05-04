## ADDED Requirements

### Requirement: Effective-Time Sort Scales Independently Of Gallery Size

Effective-time sort (the default ordering and explicit `sort=exif.dateTimeOriginal,(asc|desc)`) on `GET /apis/console.api.photo.halo.run/v1alpha1/photos` SHALL be served by an index-backed query whose memory and CPU cost per request is bounded by the requested page size, not by the total number of non-deleted photos in the gallery. The implementation SHALL NOT load every matching photo into memory in order to apply the EXIF-time-then-creation-time fallback.

#### Scenario: Large gallery does not load every photo per request

- **WHEN** a console client issues `GET /apis/console.api.photo.halo.run/v1alpha1/photos?page=1&size=20` against a gallery of 50 000 photos with `sort=exif.dateTimeOriginal,desc` (or no `sort` parameter)
- **THEN** the server responds without first loading all 50 000 photos into memory for sorting
- **THEN** the page-1 response contains 20 items in the documented effective-time order
- **THEN** repeated requests at the same page exhibit response time and heap allocation bounded by page size, not by gallery size

#### Scenario: EXIF-empty photos still interleave with EXIF-populated photos

- **WHEN** a console client issues `GET /apis/console.api.photo.halo.run/v1alpha1/photos?sort=exif.dateTimeOriginal,desc` against a gallery containing both photos with populated `exif.dateTimeOriginal` and photos without
- **THEN** photos are ordered by their effective time (`exif.dateTimeOriginal` when present, otherwise `metadata.creationTimestamp`) interleaved as a single sequence
- **THEN** photos with no EXIF time are NOT clustered in a separate tail group
