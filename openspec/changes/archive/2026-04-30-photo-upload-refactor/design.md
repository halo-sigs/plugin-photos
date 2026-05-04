## Context

当前添加图片需要经过多步表单操作：打开弹窗 → 输入名称 → 选择附件 → 选择封面 → 保存。Halo 已支持附件多尺寸缩略图，cover 字段冗余。用户期望的直接上传体验缺失。

## Goals / Non-Goals

**Goals:**
- 用户可通过拖拽或点击直接上传图片，自动创建 Photo
- 上传时自动读取 EXIF 信息并存储
- 插件设置可配置附件存储策略
- 编辑弹窗简化，显示图片预览和 EXIF

**Non-Goals:**
- 批量外部链接导入优化（放在阶段 3）
- 视频文件支持
- 图片编辑（裁剪、旋转等）

## Decisions

**1. 使用 Halo AttachmentService 而非独立存储**
- 选择：复用 Halo 的附件上传能力
- 理由：统一存储管理、自动缩略图、策略配置复用
- 替代方案：独立文件存储（增加复杂度，失去 Halo 附件生态优势）

**2. EXIF 核心字段入 PhotoSpec，完整信息入 annotations**
- 选择：make/model/lensModel/dateTimeOriginal/gps 等入 spec；完整 JSON 入 annotations
- 理由：核心字段模板可直接访问；annotations 保留完整信息便于后续扩展
- 替代方案：全部放 annotations（模板使用不便）；全部放 spec（字段爆炸）

**3. 使用 metadata-extractor 读取 EXIF**
- 选择：Drew Noakes 的 metadata-extractor
- 理由：Java 生态最成熟，支持格式广泛，维护活跃
- 替代方案：Apache Commons Imaging（维护较慢）

**4. 前端使用 UppyUpload 组件**
- 选择：Halo 官方提供的 UppyUpload（基于 @uppy/vue）
- 理由：与 Halo 控制台风格一致，已集成上传进度、多文件选择
- 替代方案：自研上传组件（重复造轮子）

**5. cover 字段保留但前端不再要求**
- 选择：后端保留 cover 字段，前端编辑弹窗移除 cover 输入
- 理由：向前兼容旧数据；缩略图通过 Halo 附件服务动态生成

## Risks / Trade-offs

- **[Risk]** 上传大文件时读取 EXIF 可能阻塞响应 → **Mitigation**：EXIF 提取在上传附件后异步进行，不阻塞上传接口返回
- **[Risk]** 非 Halo 附件的外部 URL 无法生成缩略图 → **Mitigation**：缩略图生成失败时 fallback 到原图（Halo 已支持）
- **[Risk]** 存储策略未配置时上传失败 → **Mitigation**：上传前校验配置，未配置时 Toast 提示用户去设置页配置
