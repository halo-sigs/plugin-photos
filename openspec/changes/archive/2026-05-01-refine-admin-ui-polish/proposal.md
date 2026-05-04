## Why

The photo management console has been functionally improved, but the current admin UI still feels visually uneven in daily use: toolbar density, group navigation, grid cards, list rows, empty/loading states, and modal layouts do not yet read as one coherent Halo console experience. This change creates a focused UI refinement pass so the existing capabilities feel polished, predictable, and easier to scan without introducing new backend behavior.

## What Changes

- Refine the top toolbar so search, tag filtering, view mode switching, batch state, pagination context, and primary actions have consistent alignment, spacing, and responsive behavior.
- Polish the group navigation area with clearer active state, calmer drag affordances, stable action placement, better overflow handling, and stronger empty/loading states.
- Improve photo grid cards with more stable dimensions, better hover and selection states, clearer deletion/uploading feedback, and tag/name overlays that do not compete with core photo inspection.
- Improve photo list view with denser but cleaner table spacing, aligned column widths, consistent inline-edit controls, clearer row selection, and better behavior on narrow screens.
- Rework batch-operation controls so selecting photos reveals a compact, stable-height command surface with progress and disabled states that do not shift the surrounding layout.
- Polish `PhotoEditingModal`, `PhotoUploadModal`, and `GroupEditingModal` visual hierarchy, form spacing, preview blocks, EXIF/metadata sections, action placement, and validation/error presentation.
- Normalize UI component usage around Halo component conventions and `:uno:` utilities, removing ad hoc visual treatments where they conflict with existing design system patterns.
- Add responsive and accessibility checks for the main admin photo management paths.

## Capabilities

### New Capabilities
- `admin-ui-polish`: Visual and interaction requirements for the photo management console UI refinement pass.

### Modified Capabilities
- *(none)*

## Impact

- Frontend: `console/src/views/PhotoList.vue`
- Frontend components: `console/src/components/PhotoGrid.vue`, `PhotoTable.vue`, `PhotoEditingModal.vue`, `PhotoUploadModal.vue`, `GroupEditingModal.vue`, `LazyImage.vue` if needed for state rendering
- Styling: existing UnoCSS utility usage in Vue templates; no new design dependency expected
- APIs/backend: no backend API, data model, or permission changes expected
- Verification: local console smoke test at `http://127.0.0.1:8090/console`, including desktop and narrow viewport checks
