## 1. Backend Model

- [x] 1.1 Add `PhotoExif` static inner class to `Photo.java` with all displayable EXIF fields: make, model, lensModel, software, dateTimeOriginal, fNumber, exposureTime, iso, focalLength, focalLengthIn35mm, flash, whiteBalance, exposureMode, exposureProgram, meteringMode, imageWidth, imageHeight, gpsLatitude, gpsLongitude, gpsAltitude
- [x] 1.2 Add top-level `private PhotoExif exif;` field to `Photo` (sibling to `spec`)
- [x] 1.3 Remove all EXIF fields from `Photo.PhotoSpec` (make, model, lensModel, dateTimeOriginal, gpsLatitude, gpsLongitude, gpsAltitude, imageWidth, imageHeight, iso)

## 2. Upload Service

- [x] 2.1 Add extraction methods to `PhotoUploadServiceImpl.ExifData` for the new fields: fNumber, exposureTime, focalLength, focalLengthIn35mm, flash, whiteBalance, exposureMode, exposureProgram, meteringMode, software (using appropriate `metadata-extractor` TAG constants)
- [x] 2.2 In `createPhoto()`, build a `Photo.PhotoExif` object from `ExifData` and assign it via `photo.setExif(...)` instead of populating `spec` fields
- [x] 2.3 Remove the raw EXIF serialization to `metadata.annotations["photos.halo.run/exif"]` (including `toFullMap()`, the `OBJECT_MAPPER` usage, and the annotations put call)

## 3. Value Object

- [x] 3.1 Add `Photo.PhotoExif exif` field to `PhotoVo.java`
- [x] 3.2 Populate `exif` from `photo.getExif()` in the `PhotoVo.from(Photo photo)` factory method

## 4. Frontend API Client Regeneration

- [x] 4.1 Run `./gradlew generateApiClient` to regenerate the TypeScript client reflecting the new `Photo` structure (`exif` top-level field, no EXIF fields in `spec`)

## 5. Frontend Components

- [x] 5.1 Update `PhotoEditingModal.vue`: replace all `formState.spec.make/model/lensModel/iso/imageWidth/imageHeight/gpsLatitude/gpsLongitude` references with `formState.exif.*`
- [x] 5.2 Update `PhotoEditingModal.vue`: fix the "has EXIF data" guard condition (from `spec.*` to `exif.*`) and add display cards for newly added fields (fNumber, exposureTime, focalLength, etc.)
- [x] 5.3 Update `PhotoTable.vue`: replace `photo.spec.make` / `photo.spec.model` references with `photo.exif?.make` / `photo.exif?.model`

## 6. Type Definitions

- [x] 6.1 Check `console/src/types/index.ts`: if it contains hand-written `Photo` / `PhotoSpec` types, update them to remove EXIF fields and add `exif?: PhotoExif` plus a `PhotoExif` interface; skip if types are fully generated

## 7. Verification

- [x] 7.1 Run `cd console && pnpm type-check` — no TypeScript errors
- [x] 7.2 Run `./gradlew test` — all Java unit tests pass
- [x] 7.3 Start `./gradlew haloServer`, upload a JPEG with EXIF in the browser, and confirm the detail modal correctly displays make, model, aperture, shutter speed, ISO, and focal length from `photo.exif`
- [x] 7.4 Confirm the uploaded Photo object's `metadata.annotations` does not contain the key `photos.halo.run/exif`
