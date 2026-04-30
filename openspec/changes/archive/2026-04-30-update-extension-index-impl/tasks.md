## 1. Update Index Registration

- [x] 1.1 Update `PhotoPlugin.start()` to use `IndexSpecs.single(name, keyType)` instead of `new IndexSpec()` and `IndexAttributeFactory.simpleAttribute()`
- [x] 1.2 Update `PhotoPlugin.stop()` if any changes needed for unregistering schemes
- [x] 1.3 Verify `Photo` indexes (`spec.groupName`, `spec.displayName`, `spec.priority`) use `String.class` key type
- [x] 1.4 Verify `PhotoGroup` index (`spec.priority`) uses `String.class` key type

## 2. Update Query Construction

- [x] 2.1 Update `PhotoServiceImpl.toListOptions()` to replace `QueryFactory.contains()` with `Queries.contains()`
- [x] 2.2 Update `PhotoServiceImpl.toListOptions()` to replace `QueryFactory.equal()` with `Queries.equal()`
- [x] 2.3 Update `PhotoFinderImpl.pagePhoto()` to replace `QueryFactory.equal()` with `Queries.equal()`
- [x] 2.4 Update `PhotoFinderImpl.listBy()` to replace `QueryFactory.equal()` with `Queries.equal()`
- [x] 2.5 Update `PhotoGroupServiceImpl.deletePhotoGroup()` to replace `QueryFactory.equal()` with `Queries.equal()`

## 3. Verify Compilation and Tests

- [x] 3.1 Run `./gradlew build` to verify compilation succeeds
- [x] 3.2 Run `./gradlew test` to verify all tests pass
- [x] 3.3 If compilation fails due to missing APIs, bump Halo platform version in `build.gradle`
