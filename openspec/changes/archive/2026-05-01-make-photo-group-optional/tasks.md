## 1. Backend schema relaxation

- [x] 1.1 Update `src/main/java/run/halo/photos/Photo.java` so `PhotoSpec.groupName` no longer carries `@Schema(requiredMode = REQUIRED, pattern = "^\\S+$")`; replace with a non-required schema annotation that documents the field and accepts null/empty values.
- [x] 1.2 Confirm `src/main/java/run/halo/photos/PhotoPlugin.java` index function for `spec.groupName` still returns a stable empty value when the photo has no group (current implementation already handles null spec).

## 2. Backend service and finder verification

- [x] 2.1 Verify `src/main/java/run/halo/photos/service/impl/PhotoServiceImpl.java#toListOptions` only filters by group when `query.getGroup()` is non-blank; if the console needs to express "ungrouped only", extend `PhotoQuery` and `toListOptions` with an explicit ungrouped flag/sentinel.
- [x] 2.2 Verify `src/main/java/run/halo/photos/service/impl/PhotoUploadServiceImpl.java#createPhoto` accepts an empty `groupName` argument and persists the photo without coercing it; adjust if it currently assumes non-empty.
- [x] 2.3 Verify `src/main/java/run/halo/photos/service/impl/PhotoGroupServiceImpl.java#deletePhotoGroup` cascade still scopes to `equal("spec.groupName", name)` and does not match ungrouped photos.
- [x] 2.4 Verify `src/main/java/run/halo/photos/finders/impl/PhotoFInderImpl.java` returns all photos when no group is supplied and only group-matched photos when a group name is supplied; add a finder method for ungrouped photos only if the spec calls for it.

## 3. Console endpoint changes (if needed for ungrouped filter)

- [x] 3.1 If the spec's "ungrouped only" view requires a wire-level convention, extend the console endpoint at `console.api.photo.halo.run/v1alpha1/photos` (PhotoEndpoint + PhotoQuery) to accept an explicit ungrouped indicator, ensuring the existing `group=<name>` and "no filter" semantics stay backwards compatible.
- [x] 3.2 Update OpenAPI documentation/comments for the new query parameter where applicable.

## 4. Frontend types and modal

- [x] 4.1 Update `console/src/types/index.ts` so `PhotoSpec.groupName` is optional (`groupName?: string`) and reflect the same in any helpers that pass through the field.
- [x] 4.2 Remove the `validation="required"` constraint from the group select in `console/src/components/PhotoEditingModal.vue` and add a "无分组" option (or allow clearing) so the user can save without a group.
- [x] 4.3 Adjust `PhotoEditingModal` defaults so creating a new photo defaults `groupName` to `""` (no group) instead of falling back to the currently active group when none is selected.

## 5. Frontend list view group sidebar

- [x] 5.1 Update `console/src/views/PhotoList.vue` to add a fixed virtual entry "未分组" at the top of the group sidebar; wire its selection state into `selectedGroup` (use a sentinel value such as `"__ungrouped__"` internally that the query layer translates to the wire convention from task 3.1).
- [x] 5.2 Decide and implement an "全部" entry alongside "未分组" if the design's optional decision applies; otherwise leave only "未分组".
- [x] 5.3 Remove the early-return `if (!selectedGroup.value) return []` in the photo query so the list still loads when no group is selected; replace the auto-select behavior on initial load so it picks the "未分组" / "全部" entry instead of the first group.
- [x] 5.4 Update the `useQuery` parameter mapping so the active sentinel is translated to the proper API parameters (`group=<name>` for groups, the new ungrouped indicator for ungrouped, no group filter for "全部").

## 6. Frontend upload and import flows

- [x] 6.1 Update `console/src/components/PhotoUploadModal.vue` so it does not block uploading when no group is selected; pass `groupName: ""` to the upload API in that case.
- [x] 6.2 Update the attachment-import payload builder in `console/src/views/PhotoList.vue` (`onAttachmentsSelect`) so it tolerates an ungrouped/all-photos selection without forcing a group, and so its placeholder `groupName: selectedGroup.value || ""` uses the actual ungrouped sentinel when in that view.

## 7. Frontend list mode and inline edit

- [x] 7.1 Update `console/src/components/PhotoTable.vue` inline group edit so the `<select>` includes a "无分组" option that maps to an empty string and persists clearing the value.
- [x] 7.2 Update the table cell display so a photo with empty `groupName` shows a clear "未分组" placeholder rather than an empty cell.
- [x] 7.3 Update the photo grid card overlay in `console/src/components/PhotoGrid.vue` if it currently surfaces group info; ensure ungrouped photos render correctly.

## 8. Documentation and project metadata

- [x] 8.1 Update `openspec/config.yaml` Photo description so it no longer implies `groupName` is part of the required identity (or simply note it is optional).
- [x] 8.2 Update `README.md` and any user-facing docs that describe the requirement for groups before uploading.
- [x] 8.3 Update `CLAUDE.md` if any architecture note explicitly states groupName is required.

## 9. Verification

- [x] 9.1 Run `./gradlew build` to confirm the backend and frontend compile.
- [x] 9.2 Start the dev server (`./gradlew haloServer`) and use Chrome DevTools MCP to load `http://127.0.0.1:8090/console` (login admin/admin).
- [x] 9.3 Smoke check: with zero `PhotoGroup` defined, upload a photo via the upload modal — confirm it appears in the "未分组" list.
- [x] 9.4 Smoke check: edit an existing grouped photo, clear its group, save, and confirm it now appears in the "未分组" list.
- [x] 9.5 Smoke check: in list mode, inline-edit a photo's group cell, set it to "无分组", and confirm the change persists.
- [x] 9.6 Smoke check: create a new `PhotoGroup`, assign one photo to it, then delete the group; confirm only that photo is removed and ungrouped photos remain.
- [x] 9.7 Smoke check: confirm the theme `/photos` route still renders correctly when ungrouped photos exist (no template errors from missing groupName).
