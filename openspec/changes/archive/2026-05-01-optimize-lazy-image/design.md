## Context

The current `LazyImage` component preloads images with a hidden `new Image()` inside `onMounted`, then swaps in an `<img>` tag. Every photo card in the grid triggers an immediate download as soon as Vue mounts it, so a 200-item page fires 200 parallel requests. This exhausts browser connection limits, causes main-thread jank during image decode, and makes the grid feel sluggish.

## Goals / Non-Goals

**Goals:**
- Images must load only when they are near the viewport (real lazy loading).
- Keep the existing loading spinner and error-fallback UX unchanged.
- Ensure the photo grid remains responsive with 100+ items.

**Non-Goals:**
- Replacing the grid layout or drag-and-drop behavior.
- Adding virtual scrolling or pagination.
- Changing thumbnail sizes or CDN behavior.

## Decisions

1. **Native `loading="lazy"` instead of IntersectionObserver or a library**
   - Rationale: All target browsers (Chrome, Edge, Safari, Firefox) support native lazy loading. It requires zero JS, zero dependencies, and the browser's own intersection heuristic is tuned for performance. IntersectionObserver would add boilerplate and still needs a polyfill for older browsers that the project does not target.

2. **`decoding="async"` on every image**
   - Rationale: Tells the browser it may decode the image off the main thread, preventing frame drops while thumbnails render.

3. **Drive loading/error states from the visible `<img>` element itself**
   - Rationale: The old component loaded twice (hidden Image + visible img). By binding `@load` and `@error` on the single visible `<img>`, we reduce network traffic to one request per image and eliminate the hidden preload.

4. **Keep `v-memo` on `PhotoGrid.vue` items**
   - Rationale: `v-memo` already prevents unnecessary Vue re-renders when photos are reordered; we will not touch it.

## Risks / Trade-offs

- **[Risk] Native lazy loading does not provide a "load started" event** — there can be a brief blank period before the browser begins fetching.
  → **Mitigation**: Show the loading spinner by default; it disappears once the `load` event fires. This is the same UX as today.

- **[Risk] Very fast scrolling may show blank squares momentarily** — acceptable for a management console; if it becomes problematic we can add a small `margin` via CSS `content-visibility` or an intersection-margin wrapper later.

- **[Risk] `loading="lazy"` is ignored when JavaScript is disabled** — irrelevant; the Halo console is a SPA and requires JS.
