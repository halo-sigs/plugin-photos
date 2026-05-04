## Why

The photo plugin defines two role permissions (`plugin:photos:view` and `plugin:photos:manage`) in `roleTemplate.yaml`, but the console UI only partially enforces them. Several mutation paths remain open to view-only users, creating a mismatch between backend RBAC and frontend UX.

## What Changes

- Add `v-permission="['plugin:photos:manage']"` to group edit/delete dropdown in `GroupFilter.vue`
- Add `v-permission="['plugin:photos:manage']"` to list-view checkboxes and inline editing triggers in `PhotoTable.vue`
- Conditionally render the save action in `PhotoEditingModal.vue` based on manage permission
- Ensure batch operation buttons in `PhotoList.vue` are only reachable when the user has manage permission

## Capabilities

### New Capabilities
<!-- No new capabilities introduced -->

### Modified Capabilities
<!-- No spec-level requirement changes; this is a UI enforcement fix for existing permission model -->

## Impact

- Frontend Vue components: `GroupFilter.vue`, `PhotoTable.vue`, `PhotoEditingModal.vue`, `PhotoList.vue`
- No backend API or role template changes
