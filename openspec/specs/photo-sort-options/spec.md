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
