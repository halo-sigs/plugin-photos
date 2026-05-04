## 1. Baseline Audit

- [x] 1.1 Run or open the local console and capture the current desktop layout issues for toolbar, group navigation, grid, list, and modals
- [x] 1.2 Capture the current narrow viewport issues for wrapping, clipping, overlap, and tap-target usability
- [x] 1.3 Identify which issues can be solved in existing components and which need small component extraction

## 2. Toolbar and Page Layout

- [x] 2.1 Refine `PhotoList.vue` page structure into stable header, toolbar, group navigation, content, batch, and pagination regions
- [x] 2.2 Align search, tag filter, view switch, add/upload actions, and secondary controls with consistent spacing and responsive wrapping
- [x] 2.3 Ensure loading, empty, selected, and filtered states do not cause avoidable toolbar or page layout jumps

## 3. Group Navigation

- [x] 3.1 Polish group item active, hover, dragging, deleting, and disabled states
- [x] 3.2 Improve group action placement so edit/delete and drag affordances remain predictable
- [x] 3.3 Improve group empty and loading states with clear primary actions

## 4. Photo Grid

- [x] 4.1 Refine `PhotoGrid.vue` card dimensions, spacing, image framing, hover state, and deletion feedback
- [x] 4.2 Improve grid selection indicators so selected photos are clear without obscuring previews
- [x] 4.3 Adjust grid metadata overlays for long names, multiple tags, and missing metadata

## 5. Photo List

- [x] 5.1 Refine `PhotoTable.vue` column widths, row density, thumbnail presentation, and responsive overflow behavior
- [x] 5.2 Improve selected, hover, deleting, and dragging row states
- [x] 5.3 Polish inline edit controls for name, group, and tags, including focus, save, cancel, and error states

## 6. Batch Operations

- [x] 6.1 Rework the selected-photo batch command surface into a compact stable-height region
- [x] 6.2 Add clear selected-count, progress, disabled, and completion states for batch operations
- [x] 6.3 Verify batch delete, move group, and tag updates still behave correctly after layout changes

## 7. Modals and Forms

- [x] 7.1 Polish `PhotoEditingModal.vue` preview, form sections, EXIF display, metadata section, and footer actions
- [x] 7.2 Polish `PhotoUploadModal.vue` group selection, upload area, progress feedback, and completion states
- [x] 7.3 Polish `GroupEditingModal.vue` spacing, labels, validation presentation, and actions

## 8. Verification

- [x] 8.1 Run frontend typecheck/build or the closest available console verification command
- [x] 8.2 Smoke test group selection, filtering, view switching, batch selection, drag sorting, edit modal, upload entry, and empty/loading states
- [x] 8.3 Verify desktop and narrow viewport screenshots for overlap, clipping, excessive visual noise, and unusable controls
- [x] 8.4 Document any remaining UI trade-offs or follow-up work before marking the change complete
