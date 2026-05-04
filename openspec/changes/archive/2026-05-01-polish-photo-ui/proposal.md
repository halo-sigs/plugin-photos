## Why

The photo management console UI currently has several visual inconsistencies and UX friction points that make daily use less polished than it should be: group selector layout feels cramped, batch selection causes layout jumps, the selected-photo ring persists after closing the edit modal, drag-upload area competes with a dedicated add button, and list view styling is basic. A focused polish pass will bring the interface up to the quality bar expected by Halo users.

## What Changes

- **Button icons**: Move all icon usage into Halo `VButton` icon slot instead of inline sibling elements
- **Group selector polish**: Improve spacing, alignment, and hover/active states of group pills and their action buttons
- **Batch operation toolbar**: Redesign the toolbar that appears after selecting photos to eliminate height changes and look more cohesive
- **Selection indicator**: Replace the current custom checkbox ring on photos with a cleaner selection state
- **Remove persistent selection ring**: The blue ring around a photo after opening/closing the edit modal will be eliminated (either by removing the ring entirely or clearing selection on modal close)
- **Remove drag-upload strip**: Remove the inline drag-upload area from both grid and list views
- **Unified add button with upload modal**: The top-right "新增" button becomes the single entry point with dropdown options: 直接上传, 手动添加, 从附件库选择. "直接上传" opens a modal using Halo's `UppyUpload` component and supports choosing a target group
- **List view styling**: Improve table column widths, spacing, typography, and inline editing appearance
- **Tag filter**: Add a tag filter input to the toolbar that queries photos by tag
- **Edit modal image preview**: Stabilize and improve the image preview layout in `PhotoEditingModal`

## Capabilities

### New Capabilities
- `photo-upload-modal`: Dedicated upload modal using `UppyUpload` with group selection
- `photo-tag-filter`: Filter photos by tags in the console toolbar

### Modified Capabilities
- `photo-tag-input`: Tag input styling and placement may be adjusted as part of layout polish

## Impact

- Frontend: `console/src/views/PhotoList.vue`, `console/src/components/PhotoEditingModal.vue`
- May add new component: `PhotoUploadModal.vue`
- No backend changes expected for tag filter if existing tag API supports name query parameter
