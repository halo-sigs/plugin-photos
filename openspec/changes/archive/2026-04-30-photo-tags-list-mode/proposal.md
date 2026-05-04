## Why

图库目前缺乏对照片的标签化管理能力，用户无法通过标签快速筛选和归类图片。同时，现有的网格卡片模式在批量管理（修改名称、分组、标签）时效率低下，缺少类似文件管理器的列表视图和行内编辑能力。

## What Changes

- `PhotoSpec` 新增 `tags: List<String>` 字段
- `PhotoPlugin` 中为 `spec.tags` 注册 `IndexSpecs.multi()` 索引，支持标签过滤查询
- 后端新增 `GET /apis/console.api.photo.halo.run/v1alpha1/photos/tags` 聚合接口，返回所有不重复标签列表
- 后端列表查询支持通过 `fieldSelector` 按标签过滤
- 前端 `PhotoEditingModal` 增加标签输入组件（支持自动补全已有标签）
- 前端 `PhotoList` 增加网格/列表视图切换按钮
- 列表模式使用表格展示：名称、分组、拍摄时间、相机、标签等列
- 列表模式支持行内编辑名称、分组、标签
- 标签在网格卡片上展示（有限数量）

## Capabilities

### New Capabilities
- `photo-tagging`: 为照片添加、删除、查询标签
- `photo-list-view`: 列表视图模式，支持行内编辑和批量操作

### Modified Capabilities
- *(none)*

## Impact

- 后端：`Photo.java`, `PhotoEndpoint.java`, `PhotoPlugin.java`, `PhotoService.java`
- 前端：`PhotoList.vue`, `PhotoEditingModal.vue`, 新增标签相关组件
- Finder API (`PhotoVo`) 自动包含 tags 字段，主题模板可直接使用
- 向后兼容：旧照片 tags 为 null
