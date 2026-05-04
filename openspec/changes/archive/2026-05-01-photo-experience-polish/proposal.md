## Why

图库控制台存在多个体验层面的问题：照片无法跨分组移动、批量操作能力薄弱、搜索逻辑混乱（客户端 Fuse.js 与服务端分页冲突）、照片排序不可控、空状态引导不足。这些问题叠加导致图库在照片数量增多后管理效率显著下降。

## What Changes

- `PhotoEditingModal` 增加分组选择字段，支持单张照片跨分组移动
- 前端增加批量操作栏：批量修改分组、批量添加/移除标签、批量删除
- 移除 `PhotoList` 中的 Fuse.js 客户端搜索，改为纯服务端搜索，分页与搜索状态一致
- 照片网格支持拖拽排序（类似 GroupList 的 VueDraggable），拖拽后更新 priority
- 优化空状态：未选择分组时提供直接的新建分组入口；无照片时提供上传引导
- 完善错误处理：将多处静默 `console.error` 改为 Toast 提示用户
- 修复 `isChecked` 逻辑：分离 `selectedPhoto`（编辑选中）和 `selectedPhotos`（批量勾选）的视觉状态

## Capabilities

### New Capabilities
- `photo-batch-operations`: 照片批量修改分组、标签、删除
- `photo-drag-sort`: 照片拖拽排序

### Modified Capabilities
- *(none)*

## Impact

- 前端：`PhotoList.vue`, `PhotoEditingModal.vue`, `GroupList.vue`
- 后端：`PhotoEndpoint.java`（如需批量更新接口）
- 移除前端依赖：`fuse.js`
- 无数据模型变更，完全向后兼容
