## 1. Add LazyContextVariable support to PhotoRouter

- [x] 1.1 Add `org.thymeleaf.context.LazyContextVariable` import and a static `BLOCKING_TIMEOUT` constant to `PhotoRouter.java`
- [x] 1.2 Refactor `listHandler()` to wrap `groups`, `photos`, and `title` with `LazyContextVariable`, loading via `.block(BLOCKING_TIMEOUT)`
- [x] 1.3 Refactor `renderDetail()` to wrap `neighbors`, `prev`, `next`, `position`, `total`, and `title` with `LazyContextVariable`, loading via `.block(BLOCKING_TIMEOUT)`
- [x] 1.4 Verify synchronous model entries (`group`, `page`, `size`, `photo`, `backUrl`, `photoUrl`, `_templateId`) remain unchanged

## 2. Build and verify

- [x] 2.1 Run `./gradlew build` to confirm Java compilation succeeds
- [x] 2.2 Run `./gradlew test` to ensure existing tests pass
- [ ] 2.3 (Optional) Start `./gradlew haloServer`, open the console, and test theme rendering with a template that uses the variables
- [ ] 2.4 (Optional) Test with a minimal template that does NOT reference `groups`, `photos`, `title`, `neighbors`, etc., and verify no unnecessary queries execute
