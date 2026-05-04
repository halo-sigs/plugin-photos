## 1. Pre-flight verification

- [x] 1.1 Re-run a repository-wide search for `PhotoGroupVo.from` to confirm zero callers (the only match should be the declaration itself in `src/main/java/run/halo/photos/vo/PhotoGroupVo.java:24`).
- [x] 1.2 If 1.1 surfaces any new caller introduced since this proposal, stop and revise the change rather than deleting.

## 2. Remove the dead method

- [x] 2.1 Delete the `public static PhotoGroupVoBuilder from(PhotoGroup photoGroup) { ... }` block (lines 24–30) from `src/main/java/run/halo/photos/vo/PhotoGroupVo.java`.
- [x] 2.2 Remove the now-unused `import run.halo.photos.PhotoGroup;` line from `PhotoGroupVo.java` if no other reference remains.

## 3. Verify

- [x] 3.1 Run `./gradlew build` and confirm compilation succeeds.
- [x] 3.2 Run `./gradlew test` and confirm all tests pass.
- [x] 3.3 Confirm `git diff --stat` shows changes only in `src/main/java/run/halo/photos/vo/PhotoGroupVo.java`.
