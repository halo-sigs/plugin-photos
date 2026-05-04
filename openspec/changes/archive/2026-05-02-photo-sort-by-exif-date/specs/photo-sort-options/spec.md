## ADDED Requirements

### Requirement: Console Photo List Sort Controls
管理端照片列表 SHALL 提供排序筛选控件，允许用户按创建时间或拍摄时间正/倒序排列照片。

#### Scenario: Default sort on page load
- **WHEN** 用户打开管理端照片列表
- **THEN** 列表默认以「创建时间倒序」展示照片

#### Scenario: Switch to shooting time descending
- **WHEN** 用户在排序下拉中选择「拍摄时间（新→旧）」
- **THEN** 列表按 `spec.exifData.dateTimeOriginal` 降序排列，EXIF 时间为空的照片排在末尾

#### Scenario: Switch to shooting time ascending
- **WHEN** 用户在排序下拉中选择「拍摄时间（旧→新）」
- **THEN** 列表按 `spec.exifData.dateTimeOriginal` 升序排列，EXIF 时间为空的照片排在末尾

#### Scenario: Switch to creation time ascending
- **WHEN** 用户在排序下拉中选择「创建时间（旧→新）」
- **THEN** 列表按 `metadata.creationTimestamp` 升序排列

### Requirement: Console API Sort Parameters
`GET /apis/console.api.photo.halo.run/v1alpha1/photos` SHALL 接受 `sortBy` 和 `sortOrder` 查询参数以控制排序。

#### Scenario: Sort by creation time descending (default)
- **WHEN** 请求不带 `sortBy` 参数，或 `sortBy=creationTime&sortOrder=desc`
- **THEN** 响应中照片按 `metadata.creationTimestamp` 降序排列

#### Scenario: Sort by creation time ascending
- **WHEN** 请求带 `sortBy=creationTime&sortOrder=asc`
- **THEN** 响应中照片按 `metadata.creationTimestamp` 升序排列

#### Scenario: Sort by shooting time descending
- **WHEN** 请求带 `sortBy=shootingTime&sortOrder=desc`
- **THEN** 响应中照片按 `spec.exifData.dateTimeOriginal` 降序排列，该字段为空的照片排在末尾

#### Scenario: Sort by shooting time ascending
- **WHEN** 请求带 `sortBy=shootingTime&sortOrder=asc`
- **THEN** 响应中照片按 `spec.exifData.dateTimeOriginal` 升序排列，该字段为空的照片排在末尾
