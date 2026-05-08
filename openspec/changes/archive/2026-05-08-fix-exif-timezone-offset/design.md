## Context

`PhotoUploadServiceImpl.ExifData.getDateTimeOriginal()` currently parses EXIF `DateTimeOriginal` (tag 36867) as a local-time string (`yyyy:MM:dd HH:mm:ss`) but forces `ZoneOffset.UTC` on it via `DateTimeFormatter.withZone(ZoneOffset.UTC)`. 

The EXIF standard stores the timezone offset in a separate tag — `OffsetTimeOriginal` (tag 36881). The current code never reads this tag, so photos shot in UTC+8 are stored 8 hours ahead of their actual local time.

## Goals / Non-Goals

**Goals:**
- Parse `DateTimeOriginal` using the actual timezone offset when present.
- Fall back gracefully when `OffsetTimeOriginal` is absent.

**Non-Goals:**
- No changes to data models, APIs, or frontend.
- No migration of existing photo records.

## Decisions

**Use `metadata-extractor`'s built-in `getDateOriginal()` instead of manual parsing.**

Rationale: The library already reads `OffsetTimeOriginal` (36881) and applies it when converting the string to `java.util.Date`. When the offset tag is missing, we pass `TimeZone.getDefault()` as the fallback, which is a more reasonable assumption than UTC for most deployment scenarios.

Alternative considered: Manually read `OffsetTimeOriginal` and compose `OffsetDateTime`. Rejected because the library method is well-tested and handles `SubSecTimeOriginal` (37521) automatically.

## Risks / Trade-offs

- **[Risk]** If the shooting device does not write `OffsetTimeOriginal` and the server runs in a different timezone than the shooting location, the parsed time will still be offset.
  - **Mitigation**: This is an inherent limitation of EXIF without GPS/timezone tags. The behavior is still strictly better than the current hardcoded-UTC approach.
