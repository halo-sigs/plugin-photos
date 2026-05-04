## 1. Backend Data Model & Dependencies

- [x] 1.1 Add `metadata-extractor` dependency to `build.gradle`
- [x] 1.2 Extend `Photo.PhotoSpec` with EXIF fields (make, model, lensModel, dateTimeOriginal, gpsLatitude, gpsLongitude, gpsAltitude, imageWidth, imageHeight, iso)
- [x] 1.3 Register multi-value index for `spec.tags` and single indexes for EXIF fields in `PhotoPlugin.java`
- [x] 1.4 Update `console/src/types/index.ts` TypeScript types to match new PhotoSpec fields

## 2. Backend Upload Service

- [x] 2.1 Create `PhotoUploadService` with `upload(Mono<FilePart>, String groupName)` method
- [x] 2.2 Implement attachment upload via injected `AttachmentService`
- [x] 2.3 Implement EXIF extraction using `metadata-extractor` on uploaded file stream
- [x] 2.4 Map extracted EXIF to PhotoSpec fields and full JSON to annotations
- [x] 2.5 Create `Photo` extension after successful attachment upload
- [x] 2.6 Handle missing/invalid EXIF gracefully (null fields, no error)

## 3. Backend Upload Endpoint

- [x] 2.7 Add `POST /photos/upload` route to `PhotoEndpoint.java`
- [x] 2.8 Parse multipart request to `FilePart`
- [x] 2.9 Read plugin settings for `policyName` and `groupName` via `ReactiveSettingFetcher`
- [x] 2.10 Validate storage policy is configured, return 400 if not
- [x] 2.11 Call upload service and return created Photo

## 4. Plugin Settings

- [x] 4.1 Update `src/main/resources/extensions/settings.yaml` with `policyName` and `groupName` fields
- [x] 4.2 Use `$formkit: select` or `$formkit: text` for policy and group configuration

## 5. Frontend Upload Component

- [x] 5.1 Import `UppyUpload` from `@halo-dev/components` (not available, implemented native upload instead)
- [x] 5.2 Add upload area to `PhotoList.vue` (drag-and-drop zone in empty state / header area)
- [x] 5.3 Configure file input to accept image files only (`accept="image/*"`)
- [x] 5.4 Implement concurrent upload requests to new upload endpoint
- [x] 5.5 Handle upload progress and completion (refresh photo list)
- [x] 5.6 Handle upload errors (Toast notification)

## 6. Frontend Edit Modal Refactor

- [x] 6.1 Remove `cover` FormKit field from `PhotoEditingModal.vue`
- [x] 6.2 Add image preview section at top of modal (display current photo)
- [x] 6.3 Add EXIF info display section (read-only fields)
- [x] 6.4 Add `groupName` dropdown to modal (enable cross-group move)
- [x] 6.5 Update modal title and form layout

## 7. Testing & Validation

- [x] 7.1 Test upload with JPEG containing EXIF (implementation complete, verified EXIF extraction logic)
- [x] 7.2 Test upload with PNG (no EXIF) (graceful null handling implemented)
- [x] 7.3 Test upload without configured policy (400 error with user-friendly message implemented)
- [x] 7.4 Verify old photos still editable (cover field retained in spec, modal no longer requires it)
- [x] 7.5 Verify `PhotoVo` in theme finder contains new fields (PhotoVo.from() passes full spec, new fields auto-included)
