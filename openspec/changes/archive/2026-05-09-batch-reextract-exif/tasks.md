## 1. Extract reusable EXIF parser

- [x] 1.1 Create `ExifExtractor` `@Component` in `run.halo.photos.service.impl`
- [x] 1.2 Move `extractExif(byte[])` and `ExifData` inner class from `PhotoUploadServiceImpl` to `ExifExtractor`
- [x] 1.3 Update `PhotoUploadServiceImpl` to inject and use `ExifExtractor`
- [x] 1.4 Add unit tests for `ExifExtractor` with sample image bytes

## 2. Backend API — re-extract endpoint

- [x] 2.1 Add `Mono<Photo> reextractExif(String name)` to `PhotoService` interface
- [x] 2.2 Implement `reextractExif` in `PhotoServiceImpl`
  - Fetch Photo by name
  - Query Attachment by `status.permalink = photo.spec.url`
  - Resolve local file path via `BackupRootGetter`
  - Read file bytes, call `ExifExtractor.extractExif`
  - Update `photo.exif` and save
  - Return unmodified photo if attachment missing or file unreadable
- [x] 2.3 Add `POST /photos/{name}/reextract-exif` route in `PhotoEndpoint`
- [x] 2.4 Add tests for `PhotoServiceImpl.reextractExif` (success, missing attachment, unreadable file)
- [x] 2.5 Add test for `PhotoEndpoint` route

## 3. Frontend — batch re-extract UI

- [x] 3.1 Add "重新读取 EXIF" button to batch operations toolbar in `PhotoList.vue`
- [x] 3.2 Implement `handleBatchReextractExif` using `runWithConcurrency`
- [x] 3.3 Show success/failure toast after batch completes
- [x] 3.4 Regenerate API client (`./gradlew generateApiClient`) and update imports if needed

## 4. Verification

- [x] 4.1 Build passes: `./gradlew build`
- [x] 4.2 Tests pass: `./gradlew test`
- [x] 4.3 Manual test: select photos without EXIF, click batch re-extract, verify EXIF appears
