## Why

The plugin currently forces every photo to belong to a `PhotoGroup` (`spec.groupName` is required at the schema, API, and UI levels), and the console hides photos until a group is selected. Users who only want a single rolling gallery — or who are just trying the plugin out — must create at least one group before they can upload anything, which is unnecessary friction. Making `groupName` optional lets users use the plugin without any grouping setup, while keeping groups available for those who want to organize.

## What Changes

- Relax the `Photo` schema so `spec.groupName` is optional and accepts empty/null values, including the `pattern` constraint and the `requiredMode = REQUIRED` annotation.
- Update the frontend `PhotoSpec` type and the photo editing modal so the group selector is no longer a required field; users can save a photo without picking a group.
- Update the upload flow (drag-and-drop upload modal and attachment import) so photos can be created without a group when the user has not chosen one.
- Update the photo list view in the console so that when no group is selected (or no groups exist) the user still sees their photos. Add an explicit "未分组" / all-photos option in the group sidebar so ungrouped photos remain reachable.
- Update inline edit of `groupName` in the list view so clearing the value is allowed and persists as "no group".
- Confirm backend listing, search, and finder paths already treat blank `groupName` filters as "all photos" and return ungrouped photos when filtering for unassigned ones (add an explicit "ungrouped" filter only if needed by the UI).
- Confirm that deleting a `PhotoGroup` continues to cascade only to photos whose `spec.groupName` matches that group, leaving ungrouped photos untouched.

## Capabilities

### New Capabilities
- `photo-grouping`: Behavior contract for how Photos relate to PhotoGroups across data model, console UI, upload, listing, and cascade rules — including the new "ungrouped" state.

### Modified Capabilities
- *(none)* — there is no existing main spec for grouping today; the only existing main spec is `admin-ui-polish`, which is unaffected.

## Impact

- Backend model: `src/main/java/run/halo/photos/Photo.java` (`PhotoSpec.groupName` schema annotation)
- Backend services: `src/main/java/run/halo/photos/service/impl/PhotoUploadServiceImpl.java`, `PhotoServiceImpl.java`, `PhotoGroupServiceImpl.java` (verify ungrouped handling, cascade scope)
- Backend finder: `src/main/java/run/halo/photos/finders/impl/PhotoFInderImpl.java` (verify list behaviour for null group)
- Frontend types: `console/src/types/index.ts` (`PhotoSpec.groupName`)
- Frontend views/components: `console/src/views/PhotoList.vue`, `console/src/components/PhotoEditingModal.vue`, `console/src/components/PhotoUploadModal.vue`, `console/src/components/PhotoTable.vue`
- Project description: `openspec/config.yaml` Photo summary line
- Backwards compatibility: existing photos already have a `groupName`, so this is purely a relaxation. No migration needed.
- Verification: console smoke test at `http://127.0.0.1:8090/console` covering ungrouped upload, edit, listing, group filter switching, and group delete cascade.
