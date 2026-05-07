## Why

Deleting a photo from the console only removes the `Photo` extension record — the underlying `Attachment` (and its physical file) is left behind. For users who uploaded photos through the plugin and no longer need them, this creates orphaned attachments that waste storage. However, some users may have attached the same file to other content, so auto-deletion would be destructive. The solution is to make attachment cleanup opt-in at delete time via an explicit choice.

Similarly, deleting a photo group today always cascades to delete all photos in the group, but there is no option to simply ungroup photos (move them to "ungrouped") or to clean up the attachment files alongside the photos.

## What Changes

### Photo deletion

- Add a new custom `DELETE /apis/console.api.photo.halo.run/v1alpha1/photos/{name}` endpoint with an optional `withAttachment=true` query parameter.
- When `withAttachment=false` (default): delete only the `Photo` record (same as the current auto-generated CRUD behavior).
- When `withAttachment=true`: look up the `Attachment` whose `status.permalink` equals `photo.spec.url`, delete it (triggering physical file cleanup via Halo's attachment reconciler), then delete the `Photo` record. If no matching attachment is found (e.g. photo was added manually with an external URL), silently proceed with photo-only deletion.
- In the console UI, replace the photo delete button with a `VDropdown` offering two actions:
  - **删除** — delete photo record only
  - **删除并清理附件** — delete photo record and its attachment file

### Photo group deletion

- Extend the existing `DELETE /apis/console.api.photo.halo.run/v1alpha1/photogroups/{name}` endpoint with two optional parameters:
  - `deletePhotos=true` (default `true`) — when `false`, photos in the group have their `spec.groupName` cleared (becoming ungrouped) rather than being deleted.
  - `withAttachment=false` (default) — when `true`, each photo's attachment is also deleted (only meaningful when `deletePhotos=true`).
- In the console UI, replace the group delete button with a `VDropdown` offering three actions:
  - **仅删除分组** — delete the group, move all its photos to "未分组"
  - **删除分组（含图片）** — delete the group and all its photos, keep attachment files
  - **删除分组（含图片和附件）** — delete the group, all its photos, and their attachment files

### Attachment lookup strategy

Query `Attachment` resources using `equal("status.permalink", photo.spec.url)` — this field is indexed by Halo core, so the lookup is efficient. At most one attachment should match per URL (permalinks are unique in the system). Results are processed reactively; an empty result is treated as "no attachment to clean up".

## Capabilities

### New Capabilities
- `photo-attachment-cleanup`: When deleting a photo or a group (with photos), the user can optionally delete the associated attachment files from storage.

### Modified Capabilities
- `photo-grouping`: Group deletion now supports three modes — ungroup, delete photos, delete photos with attachments.

## Impact

- Backend: `PhotoService.java`, `PhotoServiceImpl.java`, `PhotoEndpoint.java`
- Backend: `PhotoGroupService.java`, `PhotoGroupServiceImpl.java`, `PhotoGroupEndpoint.java`
- Frontend: `PhotoList.vue` or photo grid/table delete buttons → `VDropdown`
- Frontend: `GroupList.vue` delete button → `VDropdown`
- API client: regenerate via `./gradlew generateApiClient`
- Tests: `PhotoEndpointTest.java`, `PhotoGroupEndpointTest.java`, `PhotoGroupServiceImplTest.java`
