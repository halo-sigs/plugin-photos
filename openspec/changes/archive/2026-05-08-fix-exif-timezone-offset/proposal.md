## Why

EXIF `DateTimeOriginal` is parsed with a hardcoded `ZoneOffset.UTC`, treating the local-time string as UTC. This causes photos shot in non-UTC timezones (e.g., UTC+8) to display with an offset error — exactly 8 hours ahead for users in China.

## What Changes

- Fix `PhotoUploadServiceImpl.ExifData.getDateTimeOriginal()` to read the `OffsetTimeOriginal` tag instead of forcing UTC.
- Use `metadata-extractor`'s built-in `getDateOriginal()` which automatically handles `OffsetTimeOriginal`, falling back to system default timezone when the offset tag is absent.

## Capabilities

### New Capabilities

None — this is a bug fix with no new spec-level behavior.

### Modified Capabilities

None — no API contracts, data models, or requirements change.

## Impact

- `src/main/java/run/halo/photos/service/impl/PhotoUploadServiceImpl.java`
- All newly uploaded photos will have correct `dateTimeOriginal` values regardless of shooting timezone.
- Existing photos in the database are not affected (the fix only changes the upload-time parsing logic).
