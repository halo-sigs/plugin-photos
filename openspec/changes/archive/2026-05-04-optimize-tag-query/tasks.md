## 1. Narrow Tag Scan with isNotNull Filter

- [x] 1.1 In `PhotoServiceImpl.listAllTags(PhotoQuery query)`, add `.andQuery(isNotNull("spec.tags"))` to the `ListOptions` builder in `toListOptions()` when no tag filter is already applied — or unconditionally before other tag predicates — so only photos with at least one tag are deserialized.
- [x] 1.2 In `PhotoPublicQueryServiceImpl.listTags(String nameFilter)`, replace the empty `ListOptions.builder().build()` with `ListOptions.builder().andQuery(isNotNull("spec.tags")).build()`.
- [x] 1.3 Verify the `Queries.isNotNull(String field)` helper is available (it exists as `IsNotNullCondition` in the framework); add the correct static import.

## 2. Add Caffeine TTL Cache to Public listTags

- [x] 2.1 In `PhotoPublicQueryServiceImpl`, declare a `Cache<String, List<PhotoTagVo>>` field built with `Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(2)).maximumSize(100).build()`.
- [x] 2.2 At the start of `listTags(String nameFilter)`, check `cache.getIfPresent(key)` (key = nameFilter or `""` when null/blank); if present, return `Flux.fromIterable(cached)` immediately.
- [x] 2.3 After the existing `flatMapMany` pipeline, collect to list with `.collectList()`, populate the cache via `.doOnNext(list -> cache.put(key, list))`, then re-expand with `.flatMapMany(Flux::fromIterable)`.

## 3. Verify and Test

- [x] 3.1 Run `./gradlew test` and confirm all existing tests pass.
- [x] 3.2 Manually verify via the Halo console and theme `/photos` page that tag lists still display correctly.
