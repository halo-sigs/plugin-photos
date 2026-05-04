## Why

当前图库插件添加图片的流程过于繁琐：用户需要打开表单弹窗，手动输入名称、选择图片地址、再选择缩略图封面，最后保存。缩略图封面在 Halo 已支持多尺寸图片加载的场景下是冗余字段。用户期望的直接拖拽/选择上传图片、自动提取信息、一键完成添加的体验目前完全缺失。

## What Changes

- 后端新增 `POST /apis/console.api.photo.halo.run/v1alpha1/photos/upload` 接口，接收 multipart 文件，调用 Halo `AttachmentService` 上传至附件库，读取 EXIF 信息，自动创建 Photo Extension
- `PhotoSpec` 新增 EXIF 核心字段（make, model, lensModel, dateTimeOriginal, gpsLatitude, gpsLongitude, gpsAltitude, imageWidth, imageHeight, iso）
- 上传时将完整 EXIF JSON 存入 `metadata.annotations["photos.halo.run/exif"]`
- `settings.yaml` 新增附件存储策略（policyName）和附件分组（groupName）配置
- 前端引入 `@halo-dev/components` 的 `UppyUpload` 组件替换现有新增流程
- 简化 `PhotoEditingModal`：移除 cover 字段，增加图片预览区域，显示 EXIF 信息
- 保留原有 CRUD 接口和从附件库选择功能作为降级方案

## Capabilities

### New Capabilities
- `direct-photo-upload`: 直接上传图片到图库，自动转存附件库并创建 Photo
- `exif-extraction`: 读取并存储图片 EXIF 信息
- `attachment-policy-config`: 插件设置中配置附件存储策略

### Modified Capabilities
- *(none)*

## Impact

- 后端：`Photo.java`, `PhotoEndpoint.java`, `PhotoPlugin.java`, `settings.yaml`
- 新增服务类处理上传和 EXIF 提取
- 前端：`PhotoList.vue`, `PhotoEditingModal.vue`
- 新增依赖：`com.drewnoakes:metadata-extractor`（或等效库）
- 完全向后兼容：旧 Photo 数据的新字段为 null，cover 字段保留但不再要求填写
