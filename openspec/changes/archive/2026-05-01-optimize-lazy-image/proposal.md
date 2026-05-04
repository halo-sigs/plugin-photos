## Why

The current `LazyImage` component loads all images immediately on mount via a hidden `new Image()` preload, regardless of viewport visibility. In a photo grid with hundreds of items, this causes simultaneous network requests and DOM rendering that freeze or lag the page. We need real lazy loading so images only load when they approach the viewport.

## What Changes

- Replace the fake-lazy `LazyImage` component with genuine lazy-loading behavior using native `loading="lazy"` and `decoding="async"`.
- Keep loading and error slot states, but drive them from the actual `<img>` element's `load`/`error` events.
- Preserve existing styling and drag-and-drop interactions in `PhotoGrid.vue`.

## Capabilities

### New Capabilities
- `lazy-image-loading`: Native lazy-loaded image component with loading/error states, designed for large grids.

### Modified Capabilities
- (none — this is a pure implementation/performance fix with no spec-level behavior changes)

## Impact

- `console/src/components/LazyImage.vue`
- `console/src/components/PhotoGrid.vue` (no structural changes, just uses updated LazyImage)
