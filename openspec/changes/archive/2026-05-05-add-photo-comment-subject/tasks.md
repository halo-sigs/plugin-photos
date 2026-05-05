## 1. Implement PhotoCommentSubject

- [x] 1.1 Create `src/main/java/run/halo/photos/comment/PhotoCommentSubject.java`
  - Implement `CommentSubject<Photo>`
  - Inject `ReactiveExtensionClient` and `ExternalLinkProcessor`
  - Implement `get(String name)` to fetch Photo by name
  - Implement `getSubjectDisplay(String name)` to return display name, permalink `/photos/{name}`, and type label `"照片"`
  - Implement `supports(Ref ref)` to match Photo's GVK (`core.halo.run/v1alpha1/Photo`)

## 2. Verification

- [x] 2.1 Run `./gradlew build` to ensure the project compiles without errors
- [x] 2.2 Verify the plugin starts successfully and `PhotoCommentSubject` bean is registered
- [x] 2.3 Test that a comment with `subjectRef` pointing to a Photo correctly resolves the subject title and URL in Halo's comment list
