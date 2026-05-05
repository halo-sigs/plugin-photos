# 主题 API 文档

本文档介绍 plugin-photos 为主题端提供的模板路由、模板变量和 Finder API。

## 路由

### 列表页

- 模板路径：`/templates/photos.html`
- 访问路径：`/photos`

旧版路径分页 `/photos/page/{page}` 仍然可访问，但会以 `301 Moved Permanently` 永久重定向到 `/photos?page={page}`。

#### 路由可选参数

| 参数 | 说明 |
| ---- | ---- |
| `group` | 图片分组名称，对应 `PhotoGroupVo.metadata.name` |
| `page` | 分页页码（从 1 开始） |
| `size` | 每页条数 |

示例：

```
/photos?group=photo-group-UEcvi
/photos?group=photo-group-UEcvi&page=2&size=20
```

#### 模板变量

| 变量 | 类型 | 说明 |
| ---- | ---- | ---- |
| `groups` | `List<PhotoGroupVo>` | 所有分组列表 |
| `photos` | `UrlContextListResult<PhotoVo>` | 当前页图片分页结果 |
| `photoUrl` | `PhotoUrlBuilder` | URL 构造器 |
| `title` | `String` | 页面标题 |

`groups` 示例：

```html
<ul>
    <li th:each="group : ${groups}">
        <a th:href="@{${photoUrl.list(group.metadata.name)}}" th:text="${group.spec.displayName}"></a>
    </li>
</ul>
```

`photos` 示例：

```html
<ul>
    <li th:each="photo : ${photos.items}">
        <a th:href="@{${photoUrl.detail(photo)}}">
            <img th:src="${photo.spec.url}" th:alt="${photo.spec.displayName}" width="280">
        </a>
    </li>
</ul>
<div th:if="${photos.hasPrevious() || photos.hasNext()}">
    <a th:href="@{${photos.prevUrl}}"><span>上一页</span></a>
    <span th:text="${photos.page}"></span>
    <a th:href="@{${photos.nextUrl}}"><span>下一页</span></a>
</div>
```

#### PhotoUrlBuilder

`photoUrl` 是每次请求注入的 URL 构造器，不会传播白名单之外的查询参数。

| 方法 | 返回值 | 说明 |
| ---- | ---- | ---- |
| `list()` | `String` | 返回 `/photos` |
| `list(group)` | `String` | 返回 `/photos?group={group}`，`group` 为空时省略 |
| `list(group, page, size)` | `String` | 返回 `/photos?group=&page=&size=`，空白或非正值参数自动省略 |
| `detail(photo)` | `String` | 返回 `/photos/{name}`，并附加当前请求中白名单内的上下文参数（`group`、`page`、`size`） |
| `detail(photo, overrides)` | `String` | 同上，但允许通过 `Map` 覆盖单个参数值；传入空字符串可从最终 URL 中移除该参数 |

示例：

```html
<a th:href="@{${photoUrl.detail(photo)}}">查看详情</a>
<a th:href="@{${photoUrl.list(group, 1, 20)}}">回到列表第 1 页</a>
```

---

### 详情页

- 模板路径：`/templates/photo.html`
- 访问路径：`/photos/{metadata.name}`

> 注意：未提供 `photo.html` 模板的主题，访问 `/photos/{name}` 会落到 Halo 默认的"模板未找到"行为；插件本身不会返回兜底页面，由主题作者决定如何处理。

#### 路由可选参数

| 参数 | 说明 |
| ---- | ---- |
| `group` | 图片分组名称，用于回传上下文 |
| `page` | 分页页码，用于回传上下文 |
| `size` | 每页条数，用于回传上下文 |

示例：

```
/photos/photo-abc123?group=photo-group-UEcvi&page=2&size=20
```

主题作者建议使用 `${photoUrl.detail(photo)}` 生成详情链接，避免手动拼接查询字符串。

#### 模板变量

| 变量 | 类型 | 说明 |
| ---- | ---- | ---- |
| `photo` | `PhotoVo` | 当前图片 |
| `neighbors` | `List<PhotoVo>` | 同一过滤上下文内按规范排序的 5 张相邻图片（含当前图片）。靠近头/尾时窗口自动滑动以保持 5 个；列表少于 5 张时返回全部 |
| `prev` | `PhotoVo \| null` | 排序中位于当前图片之前的一张；当前为第一张时为 `null` |
| `next` | `PhotoVo \| null` | 排序中位于当前图片之后的一张；当前为最后一张时为 `null` |
| `position` | `int` | 当前图片在过滤上下文中的 1-based 序号 |
| `total` | `int` | 过滤上下文中的图片总数 |
| `group` | `String \| null` | URL 上的 `group` 查询参数（缺省时为 `null`） |
| `page` | `int` | URL 上的 `page` 查询参数（缺省为 1） |
| `size` | `int` | URL 上的 `size` 查询参数（缺省为 `base.pageSize` 设置） |
| `backUrl` | `String` | 预先构造好的回到来源列表的 URL |
| `title` | `String` | 页面标题（默认取图片 `displayName`，否则取 `base.title` 设置） |
| `_templateId` | `String` | 固定为 `"photo"` |
| `photoUrl` | `PhotoUrlBuilder` | 与列表页相同的 URL 助手 |

#### 错误与重定向行为

- 请求的图片不存在或已被软删除 → `404 Not Found`
- URL 中的 `group` 与图片实际 `spec.groupName` 不一致 → `302 Found` 重定向到 `/photos/{name}`，并去掉错误的 `group` 查询参数（其他参数保留）

#### 缩略图条示例

```html
<nav aria-label="Photo navigation">
    <a th:if="${prev != null}" th:href="@{${photoUrl.detail(prev)}}">
        <span>上一张</span>
    </a>
    <a th:if="${next != null}" th:href="@{${photoUrl.detail(next)}}">
        <span>下一张</span>
    </a>
</nav>

<div th:if="${!#lists.isEmpty(neighbors)}">
    <a th:each="neighbor : ${neighbors}" th:href="@{${photoUrl.detail(neighbor)}}">
        <img th:src="${neighbor.spec.url}" th:alt="${neighbor.spec.displayName}">
    </a>
</div>

<a th:href="@{${backUrl}}">返回列表</a>
```

---

#### 评论支持

图库图片已适配 Halo 的评论来源。在详情页模板中，可通过 `halo:comment` 标签为照片添加评论功能：

```html
<halo:comment group="core.halo.run" kind="Photo" th:attr="name=${photo.metadata.name}" />
```

参数说明：

| 属性 | 值 | 说明 |
| ---- | ---- | ---- |
| `group` | `core.halo.run` | Photo 扩展的 API group |
| `kind` | `Photo` | Photo 扩展的 kind |
| `name` | `${photo.metadata.name}` | 当前照片的 metadata name |

> 注：评论功能依赖 Halo 的 `plugin-comment-widget` 插件。如果该插件未启用，评论标签不会渲染任何内容。

---

## Finder API

Finder API 由 `photoFinder` 对象提供，可在主题模板的任意位置使用，无需依赖路由页面。

### groupBy()

获取全部分组及其图片。

**参数**：无

**返回值**：`List<PhotoGroupVo>`

**示例**：

```html
<ul>
    <li th:each="group : ${photoFinder.groupBy()}">
        <a th:href="@{${photoUrl.list(group.metadata.name)}}" th:text="${group.spec.displayName}"></a>
    </li>
</ul>
```

---

### listAll()

获取全部图片列表（无分页）。

**参数**：无

**返回值**：`List<PhotoVo>`

**示例**：

```html
<ul>
    <li th:each="photo : ${photoFinder.listAll()}" style="display: inline;">
        <img th:src="${photo.spec.url}" th:alt="${photo.spec.displayName}" width="280">
    </li>
</ul>
```

---

### listBy(group)

根据分组获取图片列表。

**参数**：

1. `group: string` — 图片分组名称，对应 `PhotoGroupVo.metadata.name`

**返回值**：`List<PhotoVo>`

**示例**：

```html
<ul>
    <li th:each="photo : ${photoFinder.listBy('photo-group-UEcvi')}" style="display: inline;">
        <img th:src="${photo.spec.url}" th:alt="${photo.spec.displayName}" width="280">
    </li>
</ul>
```

---

### list(page, size)

根据分页参数获取图片列表。

**参数**：

1. `page: int` — 分页页码，从 1 开始
2. `size: int` — 每页条数

**返回值**：`ListResult<PhotoVo>`

**示例**：

```html
<th:block th:with="photos = ${photoFinder.list(1, 10)}">
    <ul>
        <li th:each="photo : ${photos.items}">
            <img th:src="${photo.spec.url}" th:alt="${photo.spec.displayName}" width="280">
        </li>
    </ul>
    <div>
        <span th:text="${photos.page}"></span>
    </div>
</th:block>
```

---

### list(page, size, group)

根据分页参数及分组获取图片列表。

**参数**：

1. `page: int` — 分页页码，从 1 开始
2. `size: int` — 每页条数
3. `group: string` — 图片分组名称，对应 `PhotoGroupVo.metadata.name`

**返回值**：`ListResult<PhotoVo>`

**示例**：

```html
<th:block th:with="photos = ${photoFinder.list(1, 10, 'photo-group-UEcvi')}">
    <ul>
        <li th:each="photo : ${photos.items}">
            <img th:src="${photo.spec.url}" th:alt="${photo.spec.displayName}" width="280">
        </li>
    </ul>
    <div>
        <span th:text="${photos.page}"></span>
    </div>
</th:block>
```

---

## 类型定义

### PhotoVo

```json
{
  "metadata": {
    "name": "string",
    "labels": { "additionalProp1": "string" },
    "annotations": { "additionalProp1": "string" },
    "creationTimestamp": "2022-11-20T13:06:38.512Z"
  },
  "spec": {
    "displayName": "string",
    "description": "string",
    "url": "string",
    "cover": "string",
    "priority": 0,
    "groupName": "string",
    "tags": ["string"]
  },
  "exif": {
    "make": "string",
    "model": "string",
    "lensModel": "string",
    "software": "string",
    "dateTimeOriginal": "2022-11-20T13:06:38.512Z",
    "fNumber": 1.8,
    "exposureTime": "1/200",
    "iso": 100,
    "focalLength": 50.0,
    "focalLengthIn35mm": 75,
    "flash": 0,
    "whiteBalance": 0,
    "exposureMode": 0,
    "exposureProgram": 0,
    "meteringMode": 0,
    "imageWidth": 4032,
    "imageHeight": 3024
  },
  "permalink": "/photos/string"
}
```

> 注意：通过公共 API 和 `photoFinder` 返回的 `PhotoVo` 中，`exif.gpsLatitude`、`exif.gpsLongitude`、`exif.gpsAltitude` 字段已被移除，以保护隐私。未上传 EXIF 或文件不含 EXIF 时，`exif` 字段为 `null`。

### PhotoGroupVo

```json
{
  "metadata": {
    "name": "string",
    "labels": { "additionalProp1": "string" },
    "annotations": { "additionalProp1": "string" },
    "creationTimestamp": "2022-11-20T13:06:38.512Z"
  },
  "spec": {
    "displayName": "string",
    "priority": 0
  },
  "status": {
    "photoCount": 0
  },
  "photos": "List<PhotoVo>"
}
```

### ListResult\<PhotoVo>

```json
{
  "page": 0,
  "size": 0,
  "total": 0,
  "items": "List<PhotoVo>",
  "first": true,
  "last": true,
  "hasNext": true,
  "hasPrevious": true,
  "totalPages": 0
}
```

### UrlContextListResult\<PhotoVo>

```json
{
  "page": 0,
  "size": 0,
  "total": 0,
  "items": "List<PhotoVo>",
  "first": true,
  "last": true,
  "hasNext": true,
  "hasPrevious": true,
  "totalPages": 0,
  "prevUrl": "string",
  "nextUrl": "string"
}
```

### PhotoTagVo

```json
{
  "name": "string",
  "photoCount": 0
}
```

---

## Annotations 元数据适配

根据 Halo 的[元数据表单定义文档](https://docs.halo.run/developer-guide/annotations-form/)和[模型元数据文档](https://docs.halo.run/developer-guide/theme/annotations)，此插件适配了元数据表单功能。如果你需要为图片或分组添加额外的自定义字段，可参考以下 TargetRef 列表：

| 对应模型 | group | kind |
| ---- | ---- | ---- |
| 图库 | `core.halo.run` | `Photo` |
| 图库分组 | `core.halo.run` | `PhotoGroup` |
