## Why

当前图库管理端采用左右分栏布局（左侧分组列表 + 右侧照片网格），在宽屏下右侧照片区域被挤压，展示效率低。分组选择器占据了大量固定空间，而用户的核心操作是浏览和管理照片。此外，照片卡片使用 `VCard` 组件，样式较重且不够灵活，无法充分利用 TailwindCSS 进行精细定制。

## What Changes

- 重构 `PhotoList.vue`：移除左右分栏布局，改为顶部横向分组选择器 + 下方全宽照片展示区
- 分组选择器改为顶部横向标签/下拉形式，支持新建分组和分组管理
- 照片网格卡片使用纯 TailwindCSS 重写，替代 `VCard` 组件
- 网格卡片设计更紧凑：图片占主导、名称覆盖在底部、标签/选中状态轻量叠加
- 保留并适配已有功能：拖拽上传区域、批量操作工具栏、搜索框、分页、视图切换（网格/列表）
- 列表视图在布局重构后继续使用表格形式，与上方工具栏风格统一

## Capabilities

### New Capabilities
- *(none — pure UI refactor)*

### Modified Capabilities
- *(none — no spec-level behavior changes)*

## Impact

- 前端：`PhotoList.vue` 大幅重构，`GroupList.vue` 可能移除或大幅简化
- 无后端变更，无 API 变更，无数据模型变更
- 完全向后兼容
