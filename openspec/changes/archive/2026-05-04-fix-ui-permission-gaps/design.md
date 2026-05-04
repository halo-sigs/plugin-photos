## Context

The photo plugin defines two RBAC permissions in `roleTemplate.yaml`:
- `plugin:photos:view` — browse photos and groups
- `plugin:photos:manage` — create, update, delete photos and groups

The UI currently gates the "新增" dropdown, grid-view checkboxes, and the "新建分组" button with `v-permission="['plugin:photos:manage']"`. However, several mutation surfaces remain unprotected:

- Group edit/delete dropdown (`GroupFilter.vue`)
- List-view selection checkboxes (`PhotoTable.vue`)
- Inline editing of display name, group, and tags (`PhotoTable.vue`)
- Photo editing modal save action (`PhotoEditingModal.vue`)

Because `PhotoList.vue` shows batch operation buttons (delete, move, add tags) whenever `selectedCount > 0`, an ungated list-view checkbox allows view-only users to reach destructive actions.

## Goals / Non-Goals

**Goals:**
- Consistently hide all mutation UI surfaces from users who lack `plugin:photos:manage`
- Prevent view-only users from inadvertently triggering batch operations via list-view selection
- Make the edit modal read-only (or close-only) for view-only users

**Non-Goals:**
- Changing backend RBAC rules or role templates
- Adding new permissions (e.g., a separate `edit` permission)
- Changing the existing permission model (two-role view/manage is sufficient)

## Decisions

**Use `v-permission` directive, not programmatic guards**
The codebase already uses `v-permission="['plugin:photos:manage']"` on grid checkboxes and the add button. We will extend the same pattern rather than introduce a new approach (e.g., `v-if="hasManage"` computed properties). This keeps the permission checks declarative and co-located with the elements they protect.

**Make the edit modal read-only for view users**
Rather than blocking the modal entirely (users may want to inspect photo metadata), the modal will open but hide the save button when the user lacks `manage`. This is consistent with "view" semantics.

**Gate the list-view checkbox, not the batch buttons**
`PhotoList.vue` batch buttons are already implicitly gated by `selectedCount === 0` (they only appear when items are selected). The proper fix is at the source: gate the list-view checkboxes so `selectedCount` can never become non-zero for view users.

## Risks / Trade-offs

- [Risk] Wrapping the `IconMore` dropdown in `GroupFilter.vue` with `v-permission` also hides the hover affordance entirely, which may look slightly different from other group filter items. → Mitigation: wrap only the `IconMore` trigger and dropdown content, leaving the group label and count visible.
- [Risk] Inline editing in `PhotoTable.vue` is triggered by `@click` on text cells. Adding `v-permission` on the click handler area means view users lose the hover cursor styling. → Mitigation: keep the cell text as plain text (no `cursor-pointer` or hover color) for view users.
