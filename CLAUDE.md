# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This is a Halo 2.0 plugin for photo gallery management. It provides:
- Console UI for managing photos and groups under a "图库" menu
- Theme-side `/photos` route with pagination and group filtering
- Finder API (`photoFinder`) for themes to render galleries anywhere

## Tech Stack

- **Backend**: Java 17, Gradle, Spring WebFlux (reactive), Lombok, Halo plugin framework
- **Frontend**: Vue 3, TypeScript, Rsbuild, UnoCSS, pnpm
- **Key dependencies**: `@halo-dev/ui-shared`, `@halo-dev/components`, `@tanstack/vue-query`, `@halo-dev/api-client`

## Common Commands

```bash
# Install frontend dependencies (via Gradle wrapper)
./gradlew pnpmInstall

# Full build (compiles frontend then Java)
./gradlew build

# Run Java tests
./gradlew test

# Regenerate the TypeScript API client from the backend OpenAPI spec
# Run this whenever backend endpoints or DTO/Extension fields change.
# Output goes to console/src/api/generated; do not edit those files by hand.
./gradlew generateApiClient

# Frontend only (console/)
cd console
pnpm install
pnpm dev          # Development watch mode
pnpm build        # Production build
pnpm lint         # ESLint (Vue/TS)
pnpm type-check   # vue-tsc --noEmit
pnpm prettier     # Format code
```

## Project Architecture

### Backend (Java)

The backend follows Halo's plugin extension model:

- **Domain models**: `Photo` and `PhotoGroup` extend `AbstractExtension` and use `@GVK` annotations to register as Kubernetes-like custom resources under `core.halo.run/v1alpha1`.
- **Plugin lifecycle**: `PhotoPlugin` registers schemes with database indexes on startup (e.g., `spec.groupName`, `spec.displayName`, `spec.priority`) and unregisters on stop.
- **Custom endpoints**: `PhotoEndpoint` and `PhotoGroupEndpoint` implement `CustomEndpoint` and expose console APIs under `console.api.photo.halo.run/v1alpha1`.
  - `GET /apis/console.api.photo.halo.run/v1alpha1/photos` — list photos with keyword/group filters
  - `GET /apis/console.api.photo.halo.run/v1alpha1/photogroups` — list groups
  - `DELETE /apis/console.api.photo.halo.run/v1alpha1/photogroups/{name}` — delete group and its photos
- **Finders (theme API)**: `PhotoFinder` / `PhotoFinderImpl` is annotated with `@Finder("photoFinder")` and provides reactive APIs for themes: `listAll()`, `list(page, size)`, `listBy(group)`, `groupBy()`.
- **Theme routing**: `PhotoRouter` registers a Spring `RouterFunction` for `/photos` and `/photos/page/{page}`, rendering `photos.html` template with `groups`, `photos`, and `title` model attributes.
- **Service layer**: `PhotoService` / `PhotoGroupService` abstract `ReactiveExtensionClient` usage. `PhotoGroupServiceImpl.deletePhotoGroup()` cascades deletion to all photos in the group.
- **VOs**: `PhotoVo` and `PhotoGroupVo` are immutable value objects (`@Value @Builder`) used for theme-side data exposure.

All data access uses `ReactiveExtensionClient` with reactive types (`Mono`, `Flux`).

### Frontend (Vue 3 / Console)

- **Entry point**: `console/src/index.ts` uses `definePlugin()` from `@halo-dev/ui-shared` to register a route at `/photos` with permission `plugin:photos:view`.
- **Build system**: Uses `@halo-dev/ui-plugin-bundler-kit` which wraps Rsbuild. Production output goes to `src/main/resources/console`; dev output goes to `build/resources/main/console`.
- **Styling**: UnoCSS with `presetWind3` and `transformerCompileClass`. Utility classes use the `:uno:` prefix in templates (e.g., `:uno: flex gap-2`).
- **State/data fetching**: Uses `@tanstack/vue-query` (`useQuery`) for server state. `axiosInstance` from `@halo-dev/api-client` is used for HTTP.
- **Generated API client**: TypeScript clients under `console/src/api/generated/` are produced from the backend OpenAPI spec via `./gradlew generateApiClient` (configured in `build.gradle` under `haloPlugin.openApi`). Run this task whenever backend endpoints or fields (DTOs, Extension specs) change, then import the regenerated APIs/models in the console code. Never edit the generated files by hand.
- **Components**:
  - `PhotoList.vue` — main view with group sidebar + photo grid
  - `GroupList.vue` — draggable group list (uses `vue-draggable-plus`); drag reorder updates priorities via batch PUT
  - `PhotoEditingModal.vue` / `GroupEditingModal.vue` — create/edit modals
  - `LazyImage.vue` — lazy-loaded image with loading/error states
- **Types**: `console/src/types/index.ts` defines TypeScript interfaces matching the Java Extension models (`Photo`, `PhotoGroup`, `Metadata`, etc.).

## Key Conventions

- **UnoCSS classes**: Because of `transformerCompileClass`, always use the `:uno:` prefix for utility classes in Vue templates.
- **Permissions**: Console UI uses `v-permission="['plugin:photos:manage']"` for management actions. The permission strings are defined in `src/main/resources/extensions/roleTemplate.yaml`.
- **API paths**: Console frontend calls custom endpoints at `/apis/console.api.photo.halo.run/v1alpha1/...` and standard CRUD endpoints at `/apis/core.halo.run/v1alpha1/photos` and `/apis/core.halo.run/v1alpha1/photogroups`.
- **Sorting**: Default sort for photos/groups is `spec.priority ASC`, `metadata.creationTimestamp DESC`, `metadata.name ASC`.
- **Group cascade delete**: Deleting a `PhotoGroup` also deletes all `Photo` resources with matching `spec.groupName`. Photos with an empty/unset `spec.groupName` (ungrouped) are not affected.
- **Optional grouping**: `Photo.spec.groupName` is optional. An empty/unset value means the photo is "ungrouped". The console exposes a "未分组" sidebar entry for filtering and uses an `ungrouped=true` query parameter on `console.api.photo.halo.run/v1alpha1/photos` to request only ungrouped photos.

## Local Development Setup

### Running the Dev Server

If the console is not reachable, start the development server:

```bash
./gradlew haloServer
```

### Testing the UI

1. Open `http://127.0.0.1:8090/console` in the browser (use Chrome DevTools MCP)
2. Login with **admin / admin**
3. Navigate to the 图库 (Photos) plugin page

### Halo Development Mode Configuration

To develop this plugin against a running Halo instance, configure Halo with:

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
      - "/absolute/path/to/plugin-photos"
```

Then run `./gradlew build` (or `./gradlew pnpmInstall` then `./gradlew build`) and start Halo. The plugin will be loaded from source in development mode.
