## Context

图库当前没有标签系统，用户无法按主题/场景归类照片。照片管理方式只有网格卡片视图，批量编辑效率低。

## Goals / Non-Goals

**Goals:**
- 照片支持添加/删除标签
- 标签全局共享，支持按标签过滤
- 控制台支持网格/列表视图切换
- 列表模式支持行内编辑核心字段

**Non-Goals:**
- 标签层级（父标签/子标签）
- 标签颜色或图标自定义
- 智能标签自动识别（AI 打标签）

## Decisions

**1. 标签存储在 PhotoSpec.tags 而非 metadata.labels**
- 选择：`List<String> tags` 在 PhotoSpec 中
- 理由：Halo 的 metadata.labels 是 Map<String,String>，不适合存储多值标签列表；spec 字段类型更自然，且支持 IndexSpecs.multi() 索引
- 替代方案：labels（结构不匹配，Thymeleaf 模板调用复杂）

**2. 标签聚合采用服务端全量扫描**
- 选择：后端遍历所有 Photo 的 tags 字段去重返回
- 理由：实现简单，照片数量通常可控；无需引入额外的 Tag Extension 和同步逻辑
- 替代方案：独立 Tag Extension（查询快但复杂度极高）

**3. 列表视图使用 Halo VTable 组件**
- 选择：复用 `@halo-dev/components` 的表格组件
- 理由：风格统一，已集成排序、选择等功能
- 替代方案：@tanstack/vue-table（需要额外适配 Halo 样式）

**4. 行内编辑采用 blur 自动保存**
- 选择：字段修改后失去焦点时自动调用保存 API
- 理由：减少用户操作步骤，适合批量快速修改
- 替代方案：显式保存按钮（增加操作负担）

## Risks / Trade-offs

- **[Risk]** 标签数量增多后聚合接口性能下降 → **Mitigation**：标签数量通常可控（< 1000）；如需要可后续加缓存
- **[Risk]** 行内编辑频繁保存产生大量请求 → **Mitigation**：使用防抖（debounce）或批量保存
