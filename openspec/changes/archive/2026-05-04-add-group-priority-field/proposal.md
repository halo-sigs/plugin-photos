## Why

Currently, `PhotoGroup.spec.priority` exists on the backend but is hidden from users. The console hard-codes `priority: 0` when creating groups and never exposes it in the editing form. Users can only influence group order indirectly (if at all). We need to let users explicitly control display order and have the backend honor "higher number = earlier in list".

## What Changes

- Add a **priority (权重)** number input to the group creation and editing forms.
- Update the group creation modal to pass the user-supplied `priority` instead of hard-coding `0`.
- Update the group editing modal to patch `spec.priority` along with `displayName`.
- Extend `PhotoGroupFormState` type to include `priority`.
- **Backend**: Change the default group list sort from `spec.priority ASC` to `spec.priority DESC` so that larger numbers appear first.
- Regenerate the frontend API client if the OpenAPI spec changes.

## Capabilities

### New Capabilities
- `group-priority-editor`: Console forms (create & edit) expose a numeric priority field for PhotoGroup, allowing users to directly set display order.

### Modified Capabilities
- `photo-grouping`: The group list sort requirement changes — groups are now ordered by `spec.priority DESC` (higher first) instead of ASC.

## Impact

- Frontend: `GroupForm.vue`, `GroupCreationModal.vue`, `GroupEditingModal.vue`, `types/index.ts`
- Backend: `PhotoGroupServiceImpl.java` (list sort), potentially `PhotoGroupEndpoint.java`
- API client: `console/src/api/generated` (regenerate via `./gradlew generateApiClient`)
