# 开发环境搭建

## 克隆仓库

```bash
git clone git@github.com:halo-sigs/plugin-photos.git

# 或者当你 fork 之后
git clone git@github.com:{your_github_id}/plugin-photos.git
```

## 安装依赖 & 构建

```bash
cd path/to/plugin-photos

# 安装前端依赖
# macOS / Linux
./gradlew pnpmInstall

# Windows
./gradlew.bat pnpmInstall

# 构建（编译前端 + Java）
# macOS / Linux
./gradlew build

# Windows
./gradlew.bat build
```

## Halo 配置（开发模式）

修改 Halo 配置文件，使插件以开发模式从源码加载：

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

## 前端开发

```bash
cd console

pnpm install
pnpm dev          # 开发监听模式
pnpm build        # 生产构建
pnpm lint         # ESLint（Vue/TS）
pnpm type-check   # vue-tsc --noEmit
pnpm prettier     # 格式化代码
```

## 重新生成 API 客户端

后端 Endpoint 或 DTO / Extension 字段发生变更后，需要重新生成 TypeScript API 客户端：

```bash
./gradlew generateApiClient
```

生成的文件位于 `console/src/api/generated/`，**请勿手动编辑**。

## 运行测试

```bash
./gradlew test
```
