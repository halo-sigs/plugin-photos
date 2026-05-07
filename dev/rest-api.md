# REST API 文档

本文档介绍 plugin-photos 提供的 REST API，包括公共 API 和 Console API。

> **在线查看完整 Swagger 文档**：访问 [Swagger Editor](https://editor.swagger.io/)，点击左上角 **File → Import URL**，输入以下地址即可：

```
https://raw.githubusercontent.com/halo-sigs/plugin-photos/refs/heads/main/api-docs/openapi/v3_0/photosApi.json
```

## 公共 API（匿名可访问）

此插件提供了一组公共、匿名、只读的 JSON API，位于 `api.photo.halo.run/v1alpha1`，方便使用 React / Vue / Svelte 等前端框架构建客户端渲染图库的主题使用。

### 端点列表

| 端点 | 方法 | 说明 |
| ---- | ---- | ---- |
| `/apis/api.photo.halo.run/v1alpha1/photos` | `GET` | 分页列出图片，支持 `group`、`ungrouped`、`tag`、`keyword`、`labelSelector`、`fieldSelector`、`sort`、`page`、`size` 查询参数 |
| `/apis/api.photo.halo.run/v1alpha1/photos/{name}` | `GET` | 根据 `metadata.name` 获取单张图片，不存在或已软删除时返回 `404` |
| `/apis/api.photo.halo.run/v1alpha1/photogroups` | `GET` | 返回所有分组数组，按 `spec.priority` 升序排列，每项包含 `metadata`、`spec` 和 `status.photoCount`，**不返回** `photos[]`，不支持任何查询参数 |
| `/apis/api.photo.halo.run/v1alpha1/tags` | `GET` | 列出所有不重复的标签名称及对应图片数量，支持可选的 `name` 参数进行大小写不敏感模糊过滤 |

### 匿名访问说明

插件内置了 `role-template-photos-anonymous` 角色模板，会自动聚合到匿名角色（`rbac.authorization.halo.run/aggregate-to-anonymous: "true"`），因此上述端点无需登录即可访问。

该角色**不会**授予 `console.api.photo.halo.run` 的访问权限，Console 端点仍需认证。

### GPS 隐私处理

出于隐私考虑，通过公共 API 和 `photoFinder` 返回的 `PhotoVo` 中，`exif.gpsLatitude`、`exif.gpsLongitude`、`exif.gpsAltitude` 字段会被强制置为 `null`，JSON 响应中也不会包含这些字段。底层 `Photo` 扩展数据不受影响，Console 端点直接返回 `Photo` 时仍包含 GPS 数据。

### 排序说明

`/photos` 列表端点支持通过 `sort` 查询参数控制排序，格式为 `字段名,方向`，例如 `sort=exif.dateTimeOriginal,desc`。

| 排序字段 | 说明 |
| ---- | ---- |
| `exif.dateTimeOriginal` | 按拍摄时间排序；无 EXIF 时自动回退到 `metadata.creationTimestamp` |
| `metadata.creationTimestamp` | 按创建时间排序 |

不指定 `sort` 时默认按拍摄时间降序排列，兜底为创建时间降序。

## Console API（需要认证）

Console API 位于 `console.api.photo.halo.run/v1alpha1`，供 Console 前端使用，需要登录认证。

### 端点列表

| 端点 | 方法 | 说明 |
| ---- | ---- | ---- |
| `/apis/console.api.photo.halo.run/v1alpha1/photos` | `GET` | 列出图片，支持 `keyword`、`group`、`ungrouped`、`tag`、`sort`、`page`、`size` 等查询参数 |
| `/apis/console.api.photo.halo.run/v1alpha1/photos/tags` | `GET` | 列出所有标签名称（字符串数组），支持可选的 `name` 参数进行大小写不敏感模糊过滤 |
| `/apis/console.api.photo.halo.run/v1alpha1/photos/upload` | `POST` | 上传图片文件并自动创建 Photo 资源，同时提取 EXIF 信息。请求体为 `multipart/form-data`，包含 `file`（必填，图片文件）和 `group`（可选，分组名称）字段。支持 jpeg、png、webp、gif、heic、heif 格式，文件大小限制 50MB |
| `/apis/console.api.photo.halo.run/v1alpha1/photogroups` | `GET` | 返回所有分组数组，按 `spec.priority` 升序排列，每项包含 `spec.photoCount`，不支持任何查询参数 |
| `/apis/console.api.photo.halo.run/v1alpha1/photogroups/{name}` | `DELETE` | 删除指定分组及其下所有图片 |

> 注意：上传端点需要在插件设置（`base.policyName`）中配置附件存储策略，否则返回错误。

### 标准 CRUD 端点

图片和分组的增删改查还可通过 Halo 标准 Extension CRUD 端点操作：

| 端点 | 说明 |
| ---- | ---- |
| `/apis/core.halo.run/v1alpha1/photos` | 图片资源的标准 CRUD |
| `/apis/core.halo.run/v1alpha1/photogroups` | 分组资源的标准 CRUD |
