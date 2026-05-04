## 1. Extend PhotoPublicQueryService interface

- [x] 1.1 Add `Flux<PhotoVo> listAllPhotos(ListOptions options, Sort sort)` to `PhotoPublicQueryService`
- [x] 1.2 Add `Flux<PhotoGroupVo> listAllGroups(ListOptions options, Sort sort)` to `PhotoPublicQueryService`

## 2. Implement safe paged iteration in PhotoPublicQueryServiceImpl

- [x] 2.1 Implement `listAllPhotos` using `expand` + `concatMapIterable` with chunk size 500
- [x] 2.2 Implement `listAllGroups` using the same paged-iteration pattern
- [x] 2.3 Extract a private `<T> Flux<T> fetchAllPages(Function<Integer, Mono<ListResult<T>>> fetchPage)` helper if both methods share the same boilerplate

## 3. Update PhotoFinderImpl to use new all-data methods

- [x] 3.1 Replace `listAll()` oversized `PageRequestImpl.of(1, Integer.MAX_VALUE, …)` with `listAllPhotos`
- [x] 3.2 Replace `listBy(group)` oversized page request with `listAllPhotos`
- [x] 3.3 Replace `groupBy()` oversized `listGroups` and per-group `listPhotos` calls with `listAllGroups` / `listAllPhotos`

## 4. Update PhotoRouter to use new all-data methods

- [x] 4.1 Replace `loadFilteredPhotos` oversized `PageRequestImpl.of(1, Integer.MAX_VALUE, …)` with `listAllPhotos`

## 5. Verify

- [x] 5.1 Run `./gradlew build` to confirm compilation
- [x] 5.2 Run `./gradlew test` to ensure no regressions
- [x] 5.3 Confirm no `Page size must not be greater than 1000` warnings appear in test output
