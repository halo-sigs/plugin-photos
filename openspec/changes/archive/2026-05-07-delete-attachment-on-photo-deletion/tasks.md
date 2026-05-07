## 1. Backend — PhotoService

- [x] 1.1 Add `deletePhoto(String name, boolean withAttachment)` to `PhotoService` interface
- [x] 1.2 Implement in `PhotoServiceImpl`: fetch Photo → if `withAttachment`, query `Attachment` by `equal("status.permalink", photo.spec.url)` and `client.delete()` each result → then `client.delete(photo)`

## 2. Backend — PhotoEndpoint

- [x] 2.1 Add `DELETE photos/{name}` route to `PhotoEndpoint` with optional query param `withAttachment` (default `false`)
- [x] 2.2 Delegate to `photoService.deletePhoto(name, withAttachment)`, return deleted `Photo`

## 3. Backend — PhotoGroupService

- [x] 3.1 Update `deletePhotoGroup(String name)` signature to `deletePhotoGroup(String name, boolean deletePhotos, boolean withAttachment)` in `PhotoGroupService` interface
- [x] 3.2 Implement three modes in `PhotoGroupServiceImpl`:
  - `deletePhotos=false`: clear `spec.groupName` on each photo in the group (set to `""`), then delete the group
  - `deletePhotos=true, withAttachment=false`: delete each photo, then delete the group (current behavior)
  - `deletePhotos=true, withAttachment=true`: for each photo, look up and delete its `Attachment` by permalink, delete the photo, then delete the group

## 4. Backend — PhotoGroupEndpoint

- [x] 4.1 Add `deletePhotos` query param (default `true`) and `withAttachment` query param (default `false`) to `DELETE photogroups/{name}`
- [x] 4.2 Pass both params through to `photoGroupService.deletePhotoGroup(name, deletePhotos, withAttachment)`

## 5. API Client

- [x] 5.1 Run `./gradlew generateApiClient` to regenerate the TypeScript client reflecting the new endpoints and params

## 6. Frontend — Photo delete

- [x] 6.1 Locate the photo delete button(s) in `PhotoList.vue` (grid and/or table view)
- [x] 6.2 Wrap with `VDropdown` offering two items:
  - "仅删除图片" → calls `DELETE photos/{name}` (no `withAttachment`)
  - "删除图片及附件" → calls `DELETE photos/{name}?withAttachment=true`
- [x] 6.3 Both options show a confirmation dialog (`dialog.warning`) before proceeding

## 7. Frontend — Group delete

- [x] 7.1 Locate the group delete button in `GroupFilter.vue`
- [x] 7.2 Wrap with `VDropdown` offering three items:
  - "仅删除分组（图片变为未分组）" → calls `DELETE photogroups/{name}?deletePhotos=false`
  - "删除分组及图片" → calls `DELETE photogroups/{name}` (defaults)
  - "删除分组、图片及附件" → calls `DELETE photogroups/{name}?withAttachment=true`
- [x] 7.3 Each option shows a confirmation dialog before proceeding

## 8. Tests

- [x] 8.1 Update `PhotoGroupEndpointTest` for new `deletePhotoGroup` params
- [x] 8.2 Update `PhotoGroupServiceImplTest`: test all three deletion modes (ungroup / delete / delete+attachment)
- [x] 8.3 Add `PhotoEndpointTest` cases for `DELETE photos/{name}` with and without `withAttachment`
- [x] 8.4 Run `./gradlew test` — all tests pass

## 9. Build & Verification

- [x] 9.1 Run `./gradlew build` — compiles cleanly
- [x] 9.2 Run `cd console && pnpm type-check` — no TypeScript errors
