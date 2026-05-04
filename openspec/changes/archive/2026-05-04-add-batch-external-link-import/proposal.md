## Why

Currently, users can only add photos one by one via upload or manual entry. When migrating from external sources or collecting images from URLs, there is no efficient way to batch-import multiple external links at once. This feature will save users significant time by allowing them to paste a list of image URLs and automatically create photo entries in bulk.

## What Changes

- Add a new dropdown option "批量添加外链" (Batch Import External Links) under the existing "新增" (Add New) button in the PhotoList console view
- Create a new modal dialog `ExternalLinkImportModal.vue` with:
  - A group selector (dropdown to choose an existing PhotoGroup or leave ungrouped)
  - A textarea for pasting a list of external image URLs (one per line)
- On submit, parse each non-empty line as a URL, derive the photo name from the URL's filename or path, and create Photo extensions in batch
- Add backend support for batch creation if needed, or reuse existing single-create endpoints called sequentially from the frontend
- Display a success notification with the count of created photos, and handle invalid URLs gracefully

## Capabilities

### New Capabilities

- `batch-external-link-import`: Batch creation of Photo resources from a list of external URLs via a console modal, with optional group assignment and auto-generated names

### Modified Capabilities

- None

## Impact

- **Frontend**: `console/src/components/PhotoList.vue`, new `ExternalLinkImportModal.vue`, `console/src/api/generated/` (no OpenAPI changes needed if reusing existing endpoints)
- **Backend**: None if batch creation is handled via repeated calls to existing endpoints; otherwise a new batch endpoint under `console.api.photo.halo.run/v1alpha1`
- **User experience**: Reduces time to import multiple external images from minutes to seconds
- **No breaking changes** to existing APIs or data models
