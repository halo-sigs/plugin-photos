## Why

Halo's extension index API has been updated in newer versions. The current plugin uses deprecated APIs (`IndexAttributeFactory.simpleAttribute()`, `new IndexSpec()`, `QueryFactory`) that will be removed or may already cause compatibility issues. Updating to the new `IndexSpecs` and `Queries` APIs ensures future compatibility and cleaner code.

## What Changes

- **BREAKING**: Update `PhotoPlugin.start()` to use `IndexSpecs.single(name, keyType)` instead of `new IndexSpec()` and `IndexAttributeFactory.simpleAttribute()`
- Update `PhotoServiceImpl.toListOptions()` to use `Queries` instead of `QueryFactory` for field selector queries
- Update `PhotoFinderImpl` to use `Queries` instead of `QueryFactory` for all queries
- Update Gradle `build.gradle` to depend on a Halo platform version that provides the new index APIs (currently on `2.17.0-SNAPSHOT`, may need bumping)

## Capabilities

### New Capabilities
- _(none — this is a pure maintenance/compat update)_

### Modified Capabilities
- _(none — no behavioral changes to plugin functionality)_

## Impact

- `src/main/java/run/halo/photos/PhotoPlugin.java` — index registration logic
- `src/main/java/run/halo/photos/service/impl/PhotoServiceImpl.java` — query construction
- `src/main/java/run/halo/photos/finders/impl/PhotoFInderImpl.java` — query construction
- `build.gradle` — possibly update Halo platform version
