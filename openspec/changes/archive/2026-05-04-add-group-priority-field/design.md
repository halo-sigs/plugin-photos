## Context

The `PhotoGroup` Extension already has an Integer `priority` field in `PhotoGroupSpec`, and the backend indexes it. However, the console UI completely hides this field:
- `GroupCreationModal.vue` hard-codes `priority: 0` when creating a group.
- `GroupEditingModal.vue` patches only `displayName` and `annotations`, ignoring `priority`.
- `GroupForm.vue` only renders a display-name input.

Meanwhile, the default group list sort is `spec.priority ASC`, meaning smaller numbers come first. Users expect the opposite convention: larger priority = higher rank = appears earlier.

## Goals / Non-Goals

**Goals:**
- Expose a numeric priority input in both group creation and editing forms.
- Persist the user-supplied priority value on create and update.
- Change the group listing sort to `spec.priority DESC` so higher numbers display first.
- Keep the existing `creationTimestamp` and `name` tie-breakers unchanged.

**Non-Goals:**
- Drag-and-drop reordering of groups (not in scope).
- Changing photo-level priority or sorting (unchanged).
- Adding validation rules beyond a simple integer (e.g., no uniqueness constraint).

## Decisions

**Use FormKit number input for priority**
Rationale: The project already uses FormKit for all form fields. A `type="number"` input with `validation="required|number"` is consistent with existing patterns (see `PhotoForm.vue`).

**Sort direction: DESC for priority**
Rationale: The user explicitly asked for "数字越大越靠前". We will apply `Sort.by("spec.priority").descending()` (or equivalent) in the backend list query while keeping `metadata.creationTimestamp DESC` and `metadata.name ASC` as secondary/tertiary sorts.

**Patch priority via JSON Patch in edit modal**
Rationale: `GroupEditingModal.vue` already uses JSON Patch (`jsonPatchInner`) to update fields. Adding a third `{ op: "add", path: "/spec/priority", value: data.priority }` operation is the minimal, consistent change.

**No default value change in creation form**
Rationale: The number input can start empty or at `0`. If empty, the backend already treats null priority as `0` (see `PhotoPlugin` index function). We will default the form value to `0` for clarity.

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| Existing groups with non-zero priorities created via API may reorder unexpectedly when DESC sort is applied. | Acceptable — the field was never user-facing, so any existing non-default values were set programmatically and the user wants this behavior. |
| Regenerating the API client may introduce unrelated churn in `console/src/api/generated`. | Only commit the files that actually change; run `./gradlew generateApiClient` and review the diff. |

## Migration Plan

No data migration required. The `priority` column/index already exists. Deployment steps:
1. Merge code changes.
2. Rebuild plugin (`./gradlew build`).
3. Deploy to Halo instance; groups will automatically sort by the new rule.
