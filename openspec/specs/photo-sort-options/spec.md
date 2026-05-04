# photo-sort-options Specification

## Purpose
Define how photos can be sorted in the console photo list and the underlying API that supports it.

## Requirements

### Requirement: Console Photo List Sort Controls
管理端照片列表 SHALL provide sorting controls that let users order photos by creation time or shooting time in ascending or descending order. Shooting-time ordering MUST use each photo's effective photo time: `exif.dateTimeOriginal` when present, otherwise `metadata.creationTimestamp`.

#### Scenario: Default sort on page load
- **WHEN** 用户打开管理端照片列表（未传递 `sort` 参数）
- **THEN** 列表默认以有效照片时间倒序展示照片

#### Scenario: Switch to shooting time descending
- **WHEN** 用户在排序下拉中选择「拍摄时间（新→旧）」
- **THEN** 列表按有效照片时间降序排列，其中有效照片时间为 `exif.dateTimeOriginal` 或缺失 EXIF 时的 `metadata.creationTimestamp`

#### Scenario: Switch to shooting time ascending
- **WHEN** 用户在排序下拉中选择「拍摄时间（旧→新）」
- **THEN** 列表按有效照片时间升序排列，其中有效照片时间为 `exif.dateTimeOriginal` 或缺失 EXIF 时的 `metadata.creationTimestamp`

#### Scenario: Switch to creation time descending
- **WHEN** 用户在排序下拉中选择「创建时间（新→旧）」
- **THEN** 列表按 `metadata.creationTimestamp` 降序排列

#### Scenario: Switch to creation time ascending
- **WHEN** 用户在排序下拉中选择「创建时间（旧→新）」
- **THEN** 列表按 `metadata.creationTimestamp` 升序排列

### Requirement: Console API Sort Parameter
`GET /apis/console.api.photo.halo.run/v1alpha1/photos` SHALL accept a `sort` query parameter to control ordering, using the format `field,(asc|desc)`. Supported fields are `exif.dateTimeOriginal` and `metadata.creationTimestamp`. Sorting by `exif.dateTimeOriginal` MUST use each photo's effective photo time: `exif.dateTimeOriginal` when present, otherwise `metadata.creationTimestamp`.

#### Scenario: Sort by shooting time descending (default)
- **WHEN** 请求不带 `sort` 参数
- **THEN** 响应中照片按有效照片时间降序排列

#### Scenario: Sort by shooting time descending (explicit)
- **WHEN** 请求带 `sort=exif.dateTimeOriginal,desc`
- **THEN** 响应中照片按有效照片时间降序排列，EXIF 时间为空的照片使用 `metadata.creationTimestamp` 参与同一排序序列

#### Scenario: Sort by shooting time ascending
- **WHEN** 请求带 `sort=exif.dateTimeOriginal,asc`
- **THEN** 响应中照片按有效照片时间升序排列，EXIF 时间为空的照片使用 `metadata.creationTimestamp` 参与同一排序序列

#### Scenario: Sort by creation time descending
- **WHEN** 请求带 `sort=metadata.creationTimestamp,desc`
- **THEN** 响应中照片按 `metadata.creationTimestamp` 降序排列

#### Scenario: Sort by creation time ascending
- **WHEN** 请求带 `sort=metadata.creationTimestamp,asc`
- **THEN** 响应中照片按 `metadata.creationTimestamp` 升序排列

---

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
