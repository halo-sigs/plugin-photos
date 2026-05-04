## Why

The photo sorting feature (drag-to-reorder in grid, list, and group sidebar) has never worked correctly in a paginated context. Priorities assigned per-page conflict across pages, and saving order triggers N concurrent API requests. Rather than half-fix it, we are removing all frontend sorting UI until a proper design is ready. The backend `priority` fields remain untouched as they were always reserved.

## What Changes

- Remove drag-and-drop from `PhotoGrid.vue` (grid view)
- Remove drag-and-drop from `PhotoTable.vue` (list view)
- Remove drag-and-drop group reordering from `PhotoList.vue` sidebar
- Remove `handleSavePhotoOrder`, `handlePhotosUpdate`, `handleSaveGroupOrder` and related priority sorting logic
- Remove unused `vue-draggable-plus` imports
- Keep backend `Photo.priority` / `PhotoGroup.priority` fields unchanged

## Capabilities

### New Capabilities
- (none)

### Modified Capabilities
- (none — this is a removal of broken UI, not a spec-level behavior change)

## Impact

- `console/src/components/PhotoGrid.vue`
- `console/src/components/PhotoTable.vue`
- `console/src/views/PhotoList.vue`
