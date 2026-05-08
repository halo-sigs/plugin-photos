## 1. Fix EXIF DateTimeOriginal Timezone Parsing

- [x] 1.1 Update `PhotoUploadServiceImpl.ExifData.getDateTimeOriginal()` to use `dir.getDateOriginal(TimeZone.getDefault())` instead of manual `DateTimeFormatter` with hardcoded UTC
- [x] 1.2 Verify the fix handles photos with and without `OffsetTimeOriginal` tag
- [x] 1.3 Build project (`./gradlew build`) to ensure no compilation errors
