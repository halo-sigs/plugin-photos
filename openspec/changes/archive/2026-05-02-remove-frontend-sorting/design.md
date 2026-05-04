## Context

The frontend has `VueDraggable` wrappers around the photo grid, photo table, and group sidebar. Dragging updates local state and then fires one `updatePhoto` / `updatePhotoGroup` request per item to persist `spec.priority`. This breaks as soon as pagination is involved because priorities are assigned as page-local indices.

## Goals / Non-Goals

**Goals:**
- Strip all drag-to-reorder UI and related save logic from the console frontend
- Leave backend models, DB indexes, and API endpoints untouched

**Non-Goals:**
- Re-designing a correct sorting mechanism
- Removing `PhotoSorter.java` or backend `priority` fields
- Changing the default list query sort order

## Decisions

1. **Keep `VueDraggable` package in `package.json`**
   - It may be used again later when sorting is properly implemented. Removing the import statements is enough.

2. **Do not touch backend Java code**
   - The `priority` field on `Photo` and `PhotoGroup`, the indexes in `PhotoPlugin`, and `PhotoSorter.java` stay as-is. They are harmless dead code.

3. **Remove frontend priority sorting in `useQuery` results**
   - `PhotoList.vue` sorts fetched photos by `spec.priority` in the `queryFn` callback. This sorting is removed so the list displays exactly what the API returns (default creation-time order).

## Risks / Trade-offs

- **[Risk] Users who discovered the drag handles will lose that interaction**
  → **Mitigation:** The feature was broken anyway; removing it reduces confusion.
