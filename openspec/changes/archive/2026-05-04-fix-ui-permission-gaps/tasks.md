## 1. Group Filter

- [x] 1.1 Wrap group edit/delete dropdown trigger and menu in `GroupFilter.vue` with `v-permission="['plugin:photos:manage']"`

## 2. List View (PhotoTable)

- [x] 2.1 Gate the selection checkbox column in `PhotoTable.vue` with `v-permission="['plugin:photos:manage']"`
- [x] 2.2 Remove `cursor-pointer` and hover styling from inline-editable cells when user lacks manage permission
- [x] 2.3 Gate inline editing click handlers (displayName, groupName, tags) behind manage permission

## 3. Photo Editing Modal

- [x] 3.1 Conditionally render the save button in `PhotoEditingModal.vue` based on `plugin:photos:manage` permission
- [x] 3.2 Modal is read-only for view users — save button hidden; form inputs remain visible for inspection

## 4. Verification

- [x] 4.1 Run `pnpm lint` in console directory
- [x] 4.2 Run `pnpm type-check` in console directory
