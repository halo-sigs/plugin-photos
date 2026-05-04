## Why

EXIF fields are currently mixed directly into `Photo.spec` alongside user-managed fields like `displayName`, `url`, and `groupName`, creating a semantic mismatch between desired state and auto-read metadata. Additionally, the full raw EXIF payload is serialized into `metadata.annotations`, which inflates object size unnecessarily — some images carry hundreds of EXIF tags that have no display value.

## What Changes

- **BREAKING** Remove all EXIF fields from `Photo.PhotoSpec` (make, model, lensModel, dateTimeOriginal, gpsLatitude, gpsLongitude, gpsAltitude, imageWidth, imageHeight, iso)
- Add a new top-level `exif` field on `Photo`, of type `Photo.PhotoExif`, as a sibling to `spec`
- `Photo.PhotoExif` contains a complete set of displayable EXIF fields, including newly added: fNumber, exposureTime, focalLength, focalLengthIn35mm, flash, whiteBalance, exposureMode, exposureProgram, meteringMode, software
- Remove the logic in `PhotoUploadServiceImpl` that writes raw EXIF to `metadata.annotations["photos.halo.run/exif"]`
- Update `PhotoUploadServiceImpl.createPhoto()` to populate `photo.exif` instead of `photo.spec`
- Update `PhotoVo` to expose the `exif` field for theme-side consumption
- Regenerate the frontend API client via `./gradlew generateApiClient`
- Update frontend components to reference `exif.*` instead of `spec.*` for EXIF fields

## Capabilities

### New Capabilities

- `photo-exif`: A dedicated EXIF metadata object on `Photo`, auto-populated at upload time from image data, supporting optional manual editing by users. Covers camera info, shooting parameters, dimensions, and GPS location.

### Modified Capabilities

(No requirement-level changes — this is a structural refactor only.)

## Impact

**Backend**
- `Photo.java`: Remove EXIF fields from `PhotoSpec`; add `PhotoExif` inner class and top-level `exif` field
- `PhotoUploadServiceImpl.java`: Write extracted EXIF to `photo.exif`; remove annotations storage
- `PhotoVo.java`: Add `exif` field

**Frontend**
- `console/src/api/generated/`: Auto-updated after running `generateApiClient`
- `PhotoEditingModal.vue`, `PhotoTable.vue`: Update field references from `spec.*` to `exif.*`

**Data**
- Local development: Delete the `workplace` directory and restart — no migration needed
- Production migration: **Out of scope** (feature not yet released)
