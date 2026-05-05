## Why

Currently, plugin-photos has zero integration with Halo's comment system. While the plugin provides `/photos/{name}` detail pages where themes could render comment widgets, Halo's comment engine cannot recognize `Photo` as a commentable subject. This means any comment created with `subjectRef` pointing to a `Photo` would display as an unknown subject in the comment list, breaking the user experience.

Plugin-moments already demonstrates the standard pattern for integrating with Halo comments via `CommentSubject`. Photos should follow the same pattern so that themes and the comment widget can properly associate comments with individual photos.

## What Changes

- Add `PhotoCommentSubject` implementing `CommentSubject<Photo>` to register Photo as a commentable subject in Halo's comment system
- `getSubjectDisplay()` will return the photo's `displayName` as the subject title, `/photos/{name}` as the permalink, and `"照片"` as the type label
- No changes to Photo/PhotoGroup data models, VOs, endpoints, or console UI
- No comment stats, notifications, or reconcilers — this is intentionally a minimal integration (P0 only)

## Capabilities

### New Capabilities
- `photo-comment-source`: Enable Halo's comment system to recognize and resolve Photo extensions as comment subjects

### Modified Capabilities
- None

## Impact

- **New file**: `src/main/java/run/halo/photos/comment/PhotoCommentSubject.java`
- **No breaking changes** to existing APIs, data models, or theme templates
- **No new dependencies** — uses existing `run.halo.app:api` (already on platform 2.24.0)
- **Zero frontend changes** — this is purely a backend integration point
- Themes that already use `halo:comment` can immediately comment on photos once this is deployed
