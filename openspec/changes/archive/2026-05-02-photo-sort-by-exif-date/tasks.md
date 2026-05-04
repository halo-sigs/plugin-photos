## 1. 后端 — Finder 默认排序变更

- [x] 1.1 在 `PhotoFinderImpl` 中新增 `defaultComparator()` 静态方法，按 `spec.exifData.dateTimeOriginal` 降序排列，该字段为 null 时回退到 `metadata.creationTimestamp` 降序
- [x] 1.2 将 `listAll()` 方法改为在 `client.listAll(...)` 的 Flux 上调用 `.sort(defaultComparator())` 替代原有 `defaultSort()` 参数
- [x] 1.3 将 `list(page, size)` 方法改为先 `listAll()` 再手动分页（skip/take），以应用 Comparator 排序
- [x] 1.4 将 `listBy(groupName)` 方法改为在结果 Flux 上应用 `defaultComparator()`
- [x] 1.5 将 `groupBy()` 方法内部的 per-group 照片列表改为应用 `defaultComparator()`
- [x] 1.6 保留 `defaultSort()` 方法供其他仍需 Sort 对象的地方使用，或在所有调用处移除后删除

## 2. 后端 — Console API 排序参数

- [x] 2.1 在 `PhotoEndpoint` 的 photos list handler 方法签名中新增 `sortBy`（默认 `creationTime`）和 `sortOrder`（默认 `desc`）两个 `ServerRequest` 查询参数
- [x] 2.2 当 `sortBy=creationTime` 时，使用 `Sort.by(desc/asc("metadata.creationTimestamp"))` 传入 `ReactiveExtensionClient`
- [x] 2.3 当 `sortBy=shootingTime` 时，先按默认 Sort 获取数据，再在 Flux 上应用与 Finder 相同的 `shootingTimeComparator(asc/desc)` 逻辑
- [x] 2.4 提取可复用的 `PhotoSortUtils` 工具类（或在 `PhotoFinderImpl` 中提取 package-visible 静态方法）供 Endpoint 和 Finder 共用 Comparator 逻辑

## 3. 前端 — 排序 UI

- [x] 3.1 在 `PhotoList.vue` 中新增 `sortBy` 和 `sortOrder` 响应式状态，初始值为 `creationTime` / `desc`
- [x] 3.2 在照片列表工具栏区域添加排序下拉组件（使用 `@halo-dev/components` 的 VDropdown 或 Select），选项：创建时间（新→旧）、创建时间（旧→新）、拍摄时间（新→旧）、拍摄时间（旧→新）
- [x] 3.3 将 `sortBy` 和 `sortOrder` 作为查询参数传入 `GET /apis/console.api.photo.halo.run/v1alpha1/photos` 的 axios 请求
- [x] 3.4 排序状态变更时重置分页到第一页并重新触发 `useQuery` refetch
- [x] 3.5 使用 `:uno:` 前缀为排序控件添加适当的 UnoCSS 样式，与现有筛选控件视觉一致

## 4. 验证

- [x] 4.1 执行 `./gradlew build` 确认后端编译通过
- [x] 4.2 在浏览器打开 `http://127.0.0.1:8090/console`，登录后进入照片管理页，验证排序下拉正常显示且切换选项后列表顺序变化
- [x] 4.3 验证「拍摄时间」排序：有 EXIF 时间的照片按拍摄时间排列，无 EXIF 时间的照片排在末尾
- [x] 4.4 验证主题路由 `/photos` 输出的照片顺序已按拍摄时间降序排列（可通过主题模板或 API 响应确认）
