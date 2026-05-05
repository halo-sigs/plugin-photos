## Context

Plugin-photos currently has no integration with Halo's comment system. The plugin exposes `/photos/{name}` detail pages through `PhotoRouter`, but Halo's comment widget and comment list UI cannot resolve what a comment is "about" when the subject is a `Photo` because there is no `CommentSubject<Photo>` registered.

The reference implementation is `plugin-moments`, which provides:
- `MomentCommentSubject` implementing `CommentSubject<Moment>`
- `CommentReconciler` for watching new comments and emitting notifications
- `CommentNotificationReasonPublisher` for sending notifications to the moment owner
- `MomentReconciler` for auto-subscribing owners to comment notifications

For plugin-photos, we intentionally scope this to the minimal viable integration: only `CommentSubject`. This follows the Pareto principle — a single class unlocks the core capability (photos being commentable) without the complexity of notifications, stats, or reconcilers.

## Goals / Non-Goals

**Goals:**
- Register `Photo` as a commentable subject in Halo's comment system
- Provide subject display metadata (title, URL, type label) for comments on photos
- Follow the same architectural pattern as `plugin-moments` and other Halo plugins

**Non-Goals:**
- Comment count / stats on PhotoVo (no Counter integration)
- New comment notifications (no NotificationReasonEmitter integration)
- Auto-subscription for photo owners (no Subscription / NotificationCenter integration)
- Changes to Photo/PhotoGroup data models
- Changes to console UI or theme templates
- Changes to frontend API client

## Decisions

**1. Only implement CommentSubject, nothing else**
- Rationale: `CommentSubject` is the only hard requirement for Halo's comment system to recognize a subject. Reconcilers, notification publishers, and stats are additive features that can be added later without breaking changes.
- Alternative considered: Full parity with moments (CommentSubject + Reconciler + Notification + Stats). Rejected because it introduces significant complexity (owner field, sync/reactive API mixing, notification templates) for marginal value. Photos are typically managed by admins who already see all comments in the console.

**2. Place the class in a new `comment` subpackage**
- Rationale: Keeps comment-related code isolated. If we later add reconcilers or notification publishers, they naturally fit in the same package.
- Alternative considered: Place at root package `run.halo.photos`. Rejected because comment integration is a cross-cutting concern that benefits from colocation.

**3. Use `ExternalLinkProcessor` for permalink resolution**
- Rationale: `PhotoRouter` already defines `/photos/{name}` as the detail page route. `ExternalLinkProcessor` is the Halo-standard way to resolve internal paths to full URLs, matching how moments resolves `/moments/{name}`.

**4. No changes to PhotoSpec or PhotoVo**
- Rationale: CommentSubject integration is purely about Halo's comment engine resolving subjects. It does not require new fields on the Photo extension or its VO.

## Risks / Trade-offs

- **Risk**: Without stats, themes cannot display "X comments" on photo cards or detail pages → Mitigation: Stats can be added in a follow-up change without breaking anything.
- **Risk**: Without notifications, photo owners (admins) won't receive push notifications for new comments → Mitigation: Admins can still see all comments in Halo's console comment management UI.
- **Risk**: Future addition of stats/notification may require revisiting this class → Mitigation: `PhotoCommentSubject` is stateless and self-contained; adding other classes does not affect it.
