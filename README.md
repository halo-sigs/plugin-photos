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

目前此插件为主题端提供了 `/photos` 路由，模板为 `photos.html`，也提供了 [Finder API](https://docs.halo.run/developer-guide/theme/finder-apis)，可以将图库列表渲染到任何地方。

### 模板变量

#### 路由信息

- 模板路径：/templates/photos.html
- 访问路径：/photos | /photos/page/{page}

#### 路由可选参数

group: 图片分组名称, 对应 [#PhotoGroupVo](#photogroupvo).metadata.name

示例：/photos?group=photo-group-UEcvi | /photos/page/1?group=photo-group-UEcvi

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
        <img th:src="${photo.spec.url}" th:alt="${photo.spec.displayName}" width="280">
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
    "groupName": "string",                              // 分组名称，对应分组 metadata.name
  },
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

### Annotations 元数据适配

根据 Halo 的[元数据表单定义文档](https://docs.halo.run/developer-guide/annotations-form/)和[模型元数据文档](https://docs.halo.run/developer-guide/theme/annotations)，Halo 支持为部分模型的表单添加元数据表单，此插件同样适配了此功能，如果你作为主题开发者，需要为链接或者链接分组添加额外的字段，可以参考上述文档并结合下面的 TargetRef 列表进行适配。

| 对应模型   | group            | kind       |
| ---------- | ---------------- | ---------- |
| 图库       | core.halo.run | Photo       |
| 图库分组 | core.halo.run | PhotoGroup |