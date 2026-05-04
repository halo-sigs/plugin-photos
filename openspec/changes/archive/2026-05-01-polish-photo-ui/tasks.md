## 1. Button Icon Slot Cleanup

- [x] 1.1 Replace all inline icon + text patterns with VButton icon slot across PhotoList.vue
- [x] 1.2 Replace inline icon + text patterns in PhotoEditingModal.vue footer buttons

## 2. Group Selector Polish

- [x] 2.1 Improve group pill spacing, alignment, and hover/active states
- [x] 2.2 Clean up group action button (新建分组, drag handle, more menu) styling
- [x] 2.3 Verify with Chrome MCP

## 3. Batch Operation Toolbar Polish

- [x] 3.1 Redesign batch toolbar to be compact and prevent height changes
- [x] 3.2 Ensure smooth transition when toolbar appears/disappears
- [x] 3.3 Verify with Chrome MCP

## 4. Selection Indicator Polish

- [x] 4.1 Replace custom checkbox ring with cleaner selection state on photos
- [x] 4.2 Remove persistent selection ring after closing edit modal (clear selectedPhoto on close)
- [x] 4.3 Verify with Chrome MCP

## 5. Remove Drag-Upload Strip

- [x] 5.1 Remove drag-upload strip from grid view
- [x] 5.2 Remove drag-upload strip from list view
- [x] 5.3 Verify with Chrome MCP

## 6. Upload Modal with UppyUpload

- [x] 6.1 Read Halo UppyUpload docs
- [x] 6.2 Create PhotoUploadModal.vue using UppyUpload with group selector
- [x] 6.3 Wire "直接上传" in the add dropdown to open the upload modal
- [x] 6.4 Handle upload completion and refresh photo list
- [x] 6.5 Verify with Chrome MCP

## 7. List View Styling

- [x] 7.1 Improve table column widths and spacing
- [x] 7.2 Improve inline editing cell styling
- [x] 7.3 Improve typography in list rows
- [x] 7.4 Verify with Chrome MCP

## 8. Tag Filter

- [x] 8.1 Add tag filter input to toolbar using FormKit select (multiple or single)
- [x] 8.2 Filter photos by selected tag(s)
- [x] 8.3 Clear filter restores full list
- [x] 8.4 Verify with Chrome MCP

## 9. Edit Modal Image Preview

- [x] 9.1 Fix image preview layout in PhotoEditingModal.vue
- [x] 9.2 Ensure consistent positioning and sizing
- [x] 9.3 Verify with Chrome MCP
