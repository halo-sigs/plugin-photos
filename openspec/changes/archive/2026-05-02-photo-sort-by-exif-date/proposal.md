## Why

当前照片的默认排序仅依赖 `metadata.creationTimestamp`（记录创建时间），无法反映照片真实的拍摄时间。用户上传大量照片时，往往希望按拍摄时间浏览，而非上传顺序。同时管理端缺乏排序切换入口，用户无法灵活调整查看顺序。

## What Changes

- **默认排序策略调整**：前台 Finder API（`PhotoFinder`）和主题路由 `/photos` 的默认排序改为优先使用 `spec.exifData.dateTimeOriginal`（EXIF 拍摄时间），当该字段为空时回退到 `metadata.creationTimestamp`。
- **管理端新增排序筛选**：控制台照片列表增加排序下拉选项，支持：
  - 创建时间正序 / 倒序
  - 拍摄时间正序 / 倒序
- **Console API 扩展**：`GET /apis/console.api.photo.halo.run/v1alpha1/photos` 新增 `sortBy` / `sortOrder` 查询参数，后端据此动态切换排序逻辑。

## Capabilities

### New Capabilities

- `photo-sort-options`: 管理端照片列表的排序筛选功能，包含前后端参数传递与排序逻辑。

### Modified Capabilities

- `photo-grouping`: `PhotoFinder` 和主题路由中的照片排序行为发生变化（由 creationTimestamp 优先改为 dateTimeOriginal 优先）。

## Impact

- **后端**：`PhotoEndpoint`（console API 排序参数）、`PhotoFinderImpl`（默认排序）、`PhotoRouter`（路由查询排序）
- **前端**：`PhotoList.vue`（排序 UI 与参数传递）、API 调用层
- **数据**：需要 `spec.exifData.dateTimeOriginal` 字段存在；字段缺失时自动回退，不影响现有数据
- **无破坏性变更**：排序默认值变更对主题侧输出顺序有影响，但无 API 签名变更
