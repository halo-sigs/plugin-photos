## Why

EXIF extraction was recently added to the photo upload flow, so all photos uploaded before that feature have no EXIF data. Users who want EXIF display for their existing photos currently have no option other than re-uploading them one by one. A batch "re-extract EXIF" operation in the console solves this by letting users select existing photos and re-read EXIF from their original attachment files.

## What Changes

- **Backend**: New console API endpoint `POST /apis/console.api.photo.halo.run/v1alpha1/photos/{name}/reextract-exif`
  - Looks up the `Attachment` matching `photo.spec.url` via `status.permalink`
  - Resolves the attachment's local file path from the Halo working directory (`BackupRootGetter.get().getParent().resolve("attachments/...")`)
  - Reads the file, re-parses EXIF using the same logic as upload
  - Updates `photo.exif` and persists the Photo
  - Returns the updated Photo; skips gracefully when no local attachment exists
- **Backend refactor**: Extract the private EXIF parsing logic from `PhotoUploadServiceImpl` into a reusable component (e.g., `ExifExtractor`) so both upload and re-extraction share the same parsing code
- **Frontend**: Add "重新读取 EXIF" to the batch operations toolbar in `PhotoList.vue`
  - Uses the existing `useBatchOperations` / `runWithConcurrency` pattern
  - Calls the new endpoint per selected photo with concurrency control
  - Refreshes the photo list when complete

## Capabilities

### New Capabilities
<!-- No new spec-level behavior; this is a new operational action on existing Photo/EXIF capabilities -->
*(none)*

### Modified Capabilities
<!-- No requirement changes to existing specs -->
*(none)*

## Impact

- `PhotoService` / `PhotoServiceImpl`: new `reextractExif` method
- `PhotoEndpoint`: new route registration
- `PhotoUploadServiceImpl`: EXIF parsing logic extracted to shared component
- `PhotoList.vue` (console): new batch toolbar button and handler
- `console/src/api/generated/`: API client regenerated after OpenAPI spec change
- No breaking changes to existing APIs or data models
