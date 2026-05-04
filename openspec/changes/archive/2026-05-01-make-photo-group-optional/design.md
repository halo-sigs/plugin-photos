## Context

The plugin's `Photo` resource has always carried a required `spec.groupName`. The constraint is enforced at three layers:

1. **Backend schema** — `Photo.PhotoSpec.groupName` is annotated `@Schema(requiredMode = REQUIRED, pattern = "^\\S+$")`, which rejects null, empty, or whitespace values at API time.
2. **Frontend type** — `console/src/types/index.ts` declares `groupName: string`.
3. **Frontend UI** — `PhotoEditingModal.vue` uses `validation="required"` on the group select, and `PhotoList.vue` short-circuits the photo query (`if (!selectedGroup.value) return []`) and auto-selects the first group on load.

Backend listing already tolerates an empty group filter — `PhotoServiceImpl.toListOptions` and `PhotoFinderImpl.pagePhoto` both call `equal("spec.groupName", group)` only when the value is non-blank. The `PhotoPlugin` index function for `spec.groupName` already returns `""` when `spec` is null, so the index supports null/empty values today. The cascade delete in `PhotoGroupServiceImpl.deletePhotoGroup` uses `equal("spec.groupName", name)`, so it already only matches photos in the deleted group.

Stakeholders: plugin maintainers and end users who want to use the plugin as a single rolling gallery without setting up groups.

## Goals / Non-Goals

**Goals:**
- Let a Photo exist without a group at the data, API, and UI layers.
- Provide a console UX where a user can upload, view, and edit photos with zero groups configured.
- Keep group-based filtering, listing, and cascade deletion working identically for users who do create groups.
- Add an explicit way to view "ungrouped" photos so they remain reachable when groups also exist.

**Non-Goals:**
- Migrating existing photos. Existing photos already have group assignments and stay as-is.
- Changing the `PhotoGroup` resource itself, group ordering, or `photoCount`.
- Re-architecting the console layout. Group sidebar shape stays the same; only its content and filter behavior change.
- Adding a multi-group / tag-based replacement for grouping. Tags already exist for that.
- Reworking the theme `/photos` route URL structure.

## Decisions

### Decision: Drop the `requiredMode = REQUIRED` and `pattern` constraints on `groupName`
Replace `@Schema(requiredMode = REQUIRED, pattern = "^\\S+$")` on `PhotoSpec.groupName` with a plain (or `RequiredMode.NOT_REQUIRED`) annotation that documents the field but permits null, empty, and whitespace values.

**Why over alternatives:**
- Keeping the field but making the validation conditional in service code would still reject create/update payloads at the OpenAPI / extension layer before they reach our code. The constraint must come off at the schema level.
- Renaming the field or moving it to a label/annotation would break existing data and themes that already read `spec.groupName`. Backwards compatibility wins.

### Decision: Treat empty/whitespace `groupName` as "no group" everywhere
Define the canonical "ungrouped" state as `spec.groupName` being null, empty string, or whitespace-only. All filters, finders, and UI checks normalize this consistently (`StringUtils.isBlank` on the backend, `!groupName?.trim()` on the frontend). When writing, the frontend SHOULD send `""` (or omit) rather than the deprecated convention.

**Why over alternatives:**
- Picking a magic sentinel like `"__none__"` would diverge from how the index already stores null specs as `""`, and would surprise theme authors reading the field.
- Requiring null exclusively would make JSON serialization fragile (Jackson can drop null fields).

### Decision: Add an explicit "未分组" (ungrouped) sidebar entry, plus an optional "全部" entry
Render a fixed virtual entry at the top of the group sidebar for ungrouped photos. Selecting it triggers a query that filters for empty `groupName`. If a clean way to express "all photos" via the existing list endpoint exists (no group filter), add a second virtual entry; otherwise defer to a future change.

**Why over alternatives:**
- Hiding ungrouped photos when groups exist would re-create the original problem (photos that can't be reached).
- Treating "no selected group" as "all photos" looks invisible and confuses users who expect the sidebar to drive filtering. An explicit entry is more discoverable.

### Decision: Frontend filter for ungrouped uses an explicit "ungrouped=true" parameter (or `groupName=""`) at the console endpoint
The console endpoint `GET /apis/console.api.photo.halo.run/v1alpha1/photos` accepts `group=<name>` today and ignores blank values. To express "ungrouped only", introduce a query convention (`group=` empty + `ungrouped=true` flag, OR a sentinel like `group=__none__`). The exact wire convention is settled during implementation; the requirement is that the console can request "ungrouped only" without breaking the existing "all" semantics.

**Why over alternatives:**
- Keeping `group=""` to mean both "all" and "ungrouped" is ambiguous. We want both behaviors.

### Decision: Photo upload without selected group passes `groupName = ""`
The upload modal and attachment import flow already build the payload with `groupName: selectedGroup.value || ""` in some paths. We standardize on the empty string for "no group" and add a check on the backend (`PhotoUploadServiceImpl.createPhoto`) so blank input does not get coerced into an unintended value.

**Why over alternatives:**
- Forcing the UI to require a selected group during upload was the original behavior and is exactly what we are removing.

## Risks / Trade-offs

- **Risk: Existing themes that assume `photo.spec.groupName` is non-empty might break.** → Mitigation: document the change in the proposal/spec; theme APIs (`PhotoVo`, `photoFinder`) keep returning the field unchanged; theme authors must guard for empty values just like they would for any optional field.
- **Risk: The console "ungrouped" view becomes the only place a user notices a photo has no group, leading to forgotten photos.** → Mitigation: keep the ungrouped entry visually pinned at the top of the sidebar; show its photo count. Inline group edit can also re-assign quickly.
- **Risk: Index lookups by `spec.groupName` for empty value behave inconsistently across Halo versions.** → Mitigation: rely on the existing index function which already returns `""` for null specs; add a console smoke check that confirms ungrouped queries return the expected photos.
- **Trade-off: We do not migrate existing photos to "ungrouped"; users who want that must do it manually.** This keeps the change reversible and avoids surprising data changes.

## Migration Plan

1. Ship the schema relaxation (`Photo.java`) and frontend type/UI updates together. There is no data migration — existing photos keep their groups.
2. After deploying, communicate in the plugin release notes that:
   - Photos can now exist without a group.
   - The console exposes an "未分组" view.
   - Themes that assume `groupName` is non-empty should be updated to handle empty values.
3. Rollback: revert the change set; existing data remains valid because the new behavior is purely additive (the old constraint was a strict subset of the new one).

## Open Questions

- Q: Should the console show a single combined "全部" entry alongside "未分组", or only "未分组" for now?
  - Resolution path: defer to implementation; if the existing list endpoint can express "all groups including ungrouped" cheaply, add it; otherwise keep just "未分组" + per-group entries.
- Q: Should we expose a `photoFinder.listUngrouped()` method for themes?
  - Resolution path: not required by the proposal; revisit only if a concrete theme requests it.
