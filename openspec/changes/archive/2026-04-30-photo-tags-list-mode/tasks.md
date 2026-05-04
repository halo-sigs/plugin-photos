## 1. Backend Tags Support

- [x] 1.1 Add `tags: List<String>` field to `Photo.PhotoSpec`
- [x] 1.2 Register `IndexSpecs.multi("spec.tags", String.class)` index in `PhotoPlugin.java`
- [x] 1.3 Add `GET /photos/tags` endpoint to `PhotoEndpoint.java`
- [x] 1.4 Implement tags aggregation service (scan all Photos, collect unique tags)
- [x] 1.5 Update `PhotoQuery` to support tag filtering via `fieldSelector`
- [x] 1.6 Update `console/src/types/index.ts` with tags field

## 2. Frontend Tag Input Component

- [x] 2.1 Create `TagInput.vue` component with add/remove/autocomplete
- [x] 2.2 Integrate `TagInput` into `PhotoEditingModal.vue`
- [x] 2.3 Fetch existing tags for autocomplete suggestions
- [x] 2.4 Support creating new tags and selecting existing tags
- [x] 2.5 Display tags in grid card (`PhotoList.vue` grid mode)

## 3. Frontend List View

- [x] 3.1 Add view mode toggle (grid / list) to `PhotoList.vue` header
- [x] 3.2 Persist view mode preference in localStorage or URL query
- [x] 3.3 Implement list view table using Halo `VTable` or native table
- [x] 3.4 Define columns: checkbox, name, group, dateTimeOriginal, make/model, tags
- [x] 3.5 Implement inline name editing (click to edit, blur to save)
- [x] 3.6 Implement inline group editing (dropdown on click)
- [x] 3.7 Implement inline tag editing (TagInput on click)
- [x] 3.8 Add debounce to inline save to avoid excessive requests

## 4. Backend Finder & API Updates

- [x] 4.1 Verify `PhotoVo` includes tags field for theme access
- [x] 4.2 Update `PhotoServiceImpl.toListOptions` to support tag query parameters
- [x] 4.3 Update `PhotoSorter` if needed for tag-based sorting

## 5. Testing & Validation

- [ ] 5.1 Test tag creation and assignment
- [ ] 5.2 Test tag filtering in console API
- [ ] 5.3 Test tags aggregation endpoint
- [ ] 5.4 Test inline editing in list view
- [ ] 5.5 Test view mode persistence
- [ ] 5.6 Verify old photos without tags display correctly
