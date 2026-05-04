## 1. Search Fix

- [x] 1.1 Remove `fuse.js` dependency from `console/package.json`
- [x] 1.2 Remove Fuse.js setup and `searchResults` computed from `PhotoList.vue`
- [x] 1.3 Use `photos` directly for display, rely on server-side keyword filtering
- [x] 1.4 Ensure pagination `total` matches filtered results from API
- [x] 1.5 Reset page to 1 when keyword changes

## 2. Photo Cross-Group Move

- [x] 2.1 Add group dropdown to `PhotoEditingModal.vue`
- [x] 2.2 Update save logic to include `groupName` in PUT request
- [x] 2.3 Refresh photo list after group change (photo may disappear from current group view)

## 3. Batch Operations

- [x] 3.1 Implement batch operation toolbar in `PhotoList.vue` (appears when photos selected)
- [x] 3.2 Add batch delete with confirmation dialog
- [x] 3.3 Add "Move to group" dropdown in batch toolbar
- [x] 3.4 Add "Add tags" input in batch toolbar
- [x] 3.5 Implement concurrent API calls with concurrency limit (e.g., 5 at a time)
- [x] 3.6 Show progress during batch operations
- [x] 3.7 Clear selection after batch operation completes

## 4. Photo Drag Sorting

- [x] 4.1 Wrap photo grid with `VueDraggable` (same pattern as `GroupList.vue`)
- [x] 4.2 Implement `@update` handler to batch update priorities
- [x] 4.3 Send batch PUT requests to update reordered photos' priority
- [x] 4.4 Apply same drag sorting to list view rows

## 5. State Separation & Visual Fixes

- [x] 5.1 Separate `selectedPhoto` (edit navigation) from `selectedPhotos` (batch selection)
- [x] 5.2 Use distinct visual styles: blue ring for edit selection, checkmark for batch selection
- [x] 5.3 Remove `isChecked` mixed logic, create separate computed properties

## 6. Empty State & Error Handling

- [x] 6.1 Add "新建分组" button to "未选择分组" empty state
- [x] 6.2 Add upload area to "当前没有图片" empty state
- [x] 6.3 Replace silent `console.error` with `Toast.error` in all handlers
- [x] 6.4 Add user-friendly error messages for network failures

## 7. Testing & Validation

- [ ] 7.1 Test search with keyword across multiple pages
- [ ] 7.2 Test batch move 20+ photos
- [ ] 7.3 Test drag sorting and persistence after refresh
- [ ] 7.4 Test error states (network failure, permission denied)
- [ ] 7.5 Verify no visual flicker when navigating edit modal
