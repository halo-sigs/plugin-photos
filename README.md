# plugin-photos

Halo 2.0 的相册管理插件, 支持在 Console 进行管理以及为主题端提供 `/photos` 页面路由。

## 使用方式

1. 下载，目前提供以下两个下载方式：
    - GitHub Releases：访问 [Releases](https://github.com/halo-sigs/plugin-photos/releases) 下载 Assets 中的 JAR 文件。
    - Halo 应用市场：<https://halo.run/store/apps/app-BmQJW>
2. 安装，插件安装和更新方式可参考：<https://docs.halo.run/user-guide/plugins>
3. 安装完成之后，访问 Console 左侧的**图库**菜单项，即可进行管理。
4. 前台访问地址为 `/photos`，需要注意的是，此插件需要主题提供模板（photos.html）才能访问 `/photos`。

## 开发环境

```bash
git clone git@github.com:halo-sigs/plugin-photos.git

# 或者当你 fork 之后

git clone git@github.com:{your_github_id}/plugin-photos.git
```

```bash
cd path/to/plugin-photos
```

```bash
# macOS / Linux
./gradlew pnpmInstall

# Windows
./gradlew.bat pnpmInstall
```

```bash
# macOS / Linux
./gradlew build

# Windows
./gradlew.bat build
```

修改 Halo 配置文件：

```yaml
halo:
  plugin:
    runtime-mode: development
    classes-directories:
      - "build/classes"
      - "build/resources"
    lib-directories:
      - "libs"
    fixedPluginPath:
      - "/path/to/plugin-photos"
```

## 主题适配

目前此插件为主题端提供了 `/photos` 列表路由（模板为 `photos.html`）和 `/photos/{name}` 详情路由（模板为 `photo.html`），同时也提供了 [Finder API](https://docs.halo.run/developer-guide/theme/finder-apis)，可以将图库列表渲染到任何地方。

### 模板变量

#### 路由信息

- 列表模板路径：/templates/photos.html
- 列表访问路径：/photos
- 详情模板路径：/templates/photo.html
- 详情访问路径：/photos/{metadata.name}

旧版的路径分页 `/photos/page/{page}` 仍然可访问，但会以 `301 Moved Permanently` 永久重定向到新的查询字符串形式 `/photos?page={page}`，方便保留 SEO 与历史链接。

#### 路由可选参数

- `group`: 图片分组名称, 对应 [#PhotoGroupVo](#photogroupvo).metadata.name
- `page`: 分页页码（从 1 开始），仅对列表生效；详情页用于回传上下文
- `size`: 每页条数；详情页用于回传上下文

示例：

```
/photos?group=photo-group-UEcvi
/photos?group=photo-group-UEcvi&page=2&size=20
/photos/photo-abc123?group=photo-group-UEcvi&page=2&size=20
```

主题作者建议使用 `${photo.permalink}` 或 `${photoUrl.detail(photo)}` 生成详情链接，使用 `${photoUrl.list(group, page, size)}` 生成列表链接，避免手动拼接查询字符串。

> 注意：未提供 `photo.html` 模板的主题，访问 `/photos/{name}` 会落到 Halo 默认的"模板未找到"行为；插件本身不会因此返回 404 或其他兜底页面，由主题作者决定如何处理缺失的模板。

#### 变量

groups

##### 变量类型

List<[#PhotoGroupVo](#photogroupvo)>

##### 示例

```html
<th:block th:each="group : ${groups}">
    <h2 th:text="${group.spec.displayName}"></h2>
    <ul>
        <li th:each="photo : ${group.photos}">
            <img th:src="${photo.spec.url}" th:alt="${photo.spec.displayName}" width="280">
        </li>
    </ul>
</th:block>
```

#### 变量

photos

##### 变量类型

[#UrlContextListResult\<PhotoVo>](#urlcontextlistresult)

##### 示例

```html
<ul>
    <li th:each="photo : ${photos.items}">
        <a th:href="@{${photoUrl.detail(photo)}}">
            <img th:src="${photo.spec.url}" th:alt="${photo.spec.displayName}" width="280">
        </a>
    </li>
</ul>
<div th:if="${photos.hasPrevious() || photos.hasNext()}">
   <a th:href="@{${photos.prevUrl}}">
      <span>上一页</span>
   </a>
   <span th:text="${photos.page}"></span>
   <a th:href="@{${photos.nextUrl}}">
      <span>下一页</span>
   </a>
</div>
```

#### 变量

photoUrl

##### 变量类型

`PhotoUrlBuilder`（每次请求注入的 URL 构造器）

##### 方法

| 方法 | 返回值 | 说明 |
| ---- | ---- | ---- |
| `list()` | `String` | 返回 `/photos` |
| `list(group)` | `String` | 返回 `/photos?group={group}`，`group` 为空时省略 |
| `list(group, page, size)` | `String` | 返回 `/photos?group=&page=&size=`，空白或非正值参数自动省略 |
| `detail(photo)` | `String` | 返回 `/photos/{name}`，并附加当前请求中白名单内的上下文参数（`group`、`page`、`size`） |
| `detail(photo, overrides)` | `String` | 同上，但允许通过 `Map` 覆盖单个参数值；传入空字符串可以从最终 URL 中移除该参数 |

`photoUrl` 不会传播白名单之外的查询参数，主题模板只需从中获取 URL 即可，无需自行拼接查询字符串。

##### 示例

```html
<a th:href="@{${photoUrl.detail(photo)}}">查看详情</a>
<a th:href="@{${photoUrl.list(group, 1, 20)}}">回到列表第 1 页</a>
```

### 图片详情页变量

`/photos/{name}` 路由会渲染 `photo.html` 模板，并向模板注入下列变量：

| 变量 | 类型 | 说明 |
| ---- | ---- | ---- |
| `photo` | `PhotoVo` | 当前图片 |
| `neighbors` | `List<PhotoVo>` | 同一过滤上下文内、按规范排序的 5 张相邻图片（包含当前图片）。靠近列表头/尾时窗口自动滑动以保持 5 个项目；列表少于 5 张时返回全部 |
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

### Finder API

#### groupBy()

##### 描述

获取全部分组内容。

##### 参数

无

##### 返回值

List<[#PhotoGroupVo](#photogroupvo)>

##### 示例

```html
<th:block th:each="group : ${photoFinder.groupBy()}">
    <h2 th:text="${group.spec.displayName}"></h2>
    <ul>
        <li th:each="photo : ${group.photos}">
            <img th:src="${photo.spec.url}" th:alt="${photo.spec.displayName}" width="280">
        </li>
    </ul>
</th:block>
```

#### listAll()

##### 描述

获取全部图库内容。

##### 参数

无

##### 返回值

List<[#PhotoVo](#photovo)>

##### 示例

```html
<ul>
    <li th:each="photo : ${photoFinder.listAll()}" style="display: inline;">
        <img th:src="${photo.spec.url}" th:alt="${photo.spec.displayName}" width="280">
    </li>
</ul>
```

#### listBy(group)

##### 描述

根据分组获取图片列表。

##### 参数

1. `group: string` - 图片分组名称, 对应 PhotoGroupVo.metadata.name

##### 返回值

List<[#PhotoVo](#photovo)>

##### 示例

```html
<ul>
    <li th:each="photo : ${photoFinder.listBy('photo-group-UEcvi')}" style="display: inline;">
        <img th:src="${photo.spec.url}" th:alt="${photo.spec.displayName}" width="280">
    </li>
</ul>
```

#### list(page, size)

##### 描述

根据分页参数获取图片列表。

##### 参数

1. `page: int` - 分页页码，从 1 开始
2. `size: int` - 分页条数

##### 返回值

[ListResult\<PhotoVo>](#listresult-photovo)

##### 示例

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

#### list(page, size, group)

##### 描述

根据分页参数及图片所在组获取图片列表。

##### 参数

1. `page: int` - 分页页码，从 1 开始
2. `size: int` - 分页条数
3. `group: string` - 图片分组名称, 对应 PhotoGroupVo.metadata.name

##### 返回值

[ListResult\<PhotoVo>](#listresult-photovo)

##### 示例

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

### 类型定义

#### PhotoVo

```json
{
  "metadata": {
    "name": "string",                                   // 唯一标识
    "labels": {
      "additionalProp1": "string"
    },
    "annotations": {
      "additionalProp1": "string"
    },
    "creationTimestamp": "2022-11-20T13:06:38.512Z",    // 创建时间
  },
  "spec": {
    "displayName": "string",                            // 图片名称
    "description": "string",                            // 图片描述
    "url": "string",                                    // 图片链接
    "cover": "string",                                  // 封面链接
    "priority": 0,                                      // 优先级
    "groupName": "string",                              // 分组名称，对应分组 metadata.name；可选，留空表示未分组
  },
  "permalink": "/photos/string",                        // 详情页固定链接，与上下文无关
}
```

#### PhotoGroupVo

```json
{
  "metadata": {
    "name": "string",                                   // 唯一标识
    "labels": {
      "additionalProp1": "string"
    },
    "annotations": {
      "additionalProp1": "string"
    },
    "creationTimestamp": "2022-11-20T13:06:38.512Z",    // 创建时间
  },
  "spec": {
    "displayName": "string",                            // 分组名称
    "priority": 0,                                      // 分组优先级
  },
  "status": {
    "photoCount": 0,                                    // 分组下图片数量
  },
  "photos": "List<#PhotoVo>",                           // 分组下所有图片列表
}
```

#### ListResult<PhotoVo>

```json
{
  "page": 0,                                   // 当前页码
  "size": 0,                                   // 每页条数
  "total": 0,                                  // 总条数
  "items": "List<#PhotoVo>",                   // 图片列表数据
  "first": true,                               // 是否为第一页
  "last": true,                                // 是否为最后一页
  "hasNext": true,                             // 是否有下一页
  "hasPrevious": true,                         // 是否有上一页
  "totalPages": 0                              // 总页数
}
```

#### UrlContextListResult<PhotoVo>

```json
{
  "page": 0,                                   // 当前页码
  "size": 0,                                   // 每页条数
  "total": 0,                                  // 总条数
  "items": "List<#PhotoVo>",                   // 图片列表数据
  "first": true,                               // 是否为第一页
  "last": true,                                // 是否为最后一页
  "hasNext": true,                             // 是否有下一页
  "hasPrevious": true,                         // 是否有上一页
  "totalPages": 0,                             // 总页数
  "prevUrl": "string",                         // 上一页链接
  "nextUrl": "string"                          // 下一页链接
}
```

## Public API

此插件额外提供了一组公共、匿名、只读的 JSON API，位于 `api.photo.halo.run/v1alpha1`，方便使用 React/Vue/Svelte 等前端框架构建客户端渲染图库的主题使用。

### 端点列表

| 端点 | 方法 | 说明 |
| ---- | ---- | ---- |
| `/apis/api.photo.halo.run/v1alpha1/photos` | `GET` | 分页列出图片，支持 `group`、`ungrouped`、`tag`、`keyword`、`labelSelector`、`fieldSelector`、`sort`、`page`、`size` 查询参数 |
| `/apis/api.photo.halo.run/v1alpha1/photos/{name}` | `GET` | 根据 `metadata.name` 获取单张图片，不存在或已软删除时返回 `404` |
| `/apis/api.photo.halo.run/v1alpha1/photogroups` | `GET` | 分页列出分组，返回 `metadata`、`spec` 和 `status.photoCount`，**不返回** `photos[]` |
| `/apis/api.photo.halo.run/v1alpha1/tags` | `GET` | 列出所有不重复的标签名称及对应图片数量，支持可选的 `name` 参数进行大小写不敏感模糊过滤 |

### 匿名访问

插件内置了 `role-template-photos-anonymous` 角色模板，会自动聚合到匿名角色（`rbac.authorization.halo.run/aggregate-to-anonymous: "true"`），因此上述端点无需登录即可访问。但该角色**不会**授予 `console.api.photo.halo.run` 的访问权限，Console 端点仍需要认证。

### GPS 隐私

出于隐私考虑，通过 Public API 和 `photoFinder` 返回的 `PhotoVo` 中，`exif.gpsLatitude`、`exif.gpsLongitude`、`exif.gpsAltitude` 字段会被强制置为 `null`，JSON 响应中也不会包含这些字段。底层 `Photo` 扩展数据不受影响，Console 端点直接返回 `Photo` 时仍包含 GPS 数据。

### Annotations 元数据适配

根据 Halo 的[元数据表单定义文档](https://docs.halo.run/developer-guide/annotations-form/)和[模型元数据文档](https://docs.halo.run/developer-guide/theme/annotations)，Halo 支持为部分模型的表单添加元数据表单，此插件同样适配了此功能，如果你作为主题开发者，需要为链接或者链接分组添加额外的字段，可以参考上述文档并结合下面的 TargetRef 列表进行适配。

| 对应模型   | group            | kind       |
| ---------- | ---------------- | ---------- |
| 图库       | core.halo.run | Photo       |
| 图库分组 | core.halo.run | PhotoGroup |