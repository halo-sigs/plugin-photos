## Context

照片插件目前的排序策略（`spec.priority ASC, metadata.creationTimestamp DESC, metadata.name ASC`）是通用的 CRUD 排序，无法反映照片真实拍摄时间。`Photo.spec.exifData.dateTimeOriginal` 字段（`java.time.Instant`）在上传时已从 EXIF 解析并写入，但尚未参与任何排序逻辑。

当前状态：
- `PhotoFinderImpl.defaultSort()` 返回基于字段的 Spring Data `Sort` 对象，不支持空值回退
- 管理端 `PhotoEndpoint` 的 list API 无排序参数
- `PhotoRouter`（主题路由）直接使用 finder 的默认排序

约束：
- Halo 的 `ReactiveExtensionClient.listAll()` / `page()` 接受 `Sort` 对象，但 `Sort` 对字段为 null 的记录排序行为未定义（通常 null 排在最后或最前，取决于底层实现）
- 数据库索引在 `PhotoPlugin.start()` 注册，新增可排序字段需要在此注册对应索引

## Goals / Non-Goals

**Goals:**
- Finder API (`listAll`, `list`, `listBy`, `groupBy`) 默认按拍摄时间降序排列，exifData.dateTimeOriginal 为空时回退到 metadata.creationTimestamp
- 主题路由 `/photos` 跟随 Finder 默认排序变更
- 管理端 console API 支持 `sortBy`（`creationTime` | `shootingTime`）和 `sortOrder`（`asc` | `desc`）参数
- 管理端 `PhotoList.vue` 新增排序下拉 UI，可选：创建时间正/倒序、拍摄时间正/倒序

**Non-Goals:**
- 修改 `spec.priority` 手动排序逻辑（仍保留，与新排序并列）
- 对 EXIF 解析流程做任何改动
- 主题路由暴露排序参数（主题侧只消费默认排序）

## Decisions

### 决策 1：Flux Comparator 而非 Sort 字段排序（Finder 层）

**选项**：
- A. 在 Flux 流上 `.sort(Comparator)` 实现空值回退
- B. 新增冗余字段 `spec.sortTime`（始终写入），用 Sort 对象排序

**选择**：A — Comparator 排序。

**理由**：B 需要写入时副作用（hook 或监听器），增加维护成本；A 在 `listAll()` 返回完整 Flux 后直接应用 Comparator，简单且无数据迁移。代价是 page 方法需先 listAll 再手动切片，性能与现有实现相同（底层已全量加载）。

Comparator 逻辑：
```java
Comparator.comparing(
    photo -> Optional.ofNullable(photo.getSpec().getExifData())
        .map(ExifData::getDateTimeOriginal)
        .orElse(photo.getMetadata().getCreationTimestamp()),
    Comparator.nullsLast(Comparator.reverseOrder())
)
```

### 决策 2：Console API 新增查询参数

管理端 `GET /apis/console.api.photo.halo.run/v1alpha1/photos` 增加：
- `sortBy`: `creationTime`（默认）| `shootingTime`
- `sortOrder`: `desc`（默认）| `asc`

`sortBy=creationTime` 使用 `metadata.creationTimestamp` 字段排序（通过 Sort 对象）；`sortBy=shootingTime` 使用 Flux Comparator 逻辑（与 Finder 相同）。

### 决策 3：主题路由不暴露排序参数

主题路由调用 `photoFinder` 时使用新的默认排序，不新增路由参数，保持主题 API 稳定。

## Risks / Trade-offs

- **排序变更影响现有主题输出顺序** → 属于有意为之的行为变更，在 Changelog 中标注
- **Comparator 排序需全量加载数据到内存** → 当前分页实现已如此，风险不变；照片数据量通常不大
- **exifData 字段缺失**（旧照片上传时未解析 EXIF）→ Comparator 已处理 null 回退到 creationTimestamp，安全
- **新的 `spec.exifData.dateTimeOriginal` 索引** → 若注册索引需要在 `PhotoPlugin.start()` 中添加，需确保升级时不破坏现有数据；若底层不支持按嵌套字段索引则退化为全量扫描排序（可接受）
