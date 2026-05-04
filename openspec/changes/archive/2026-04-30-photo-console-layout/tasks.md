## 1. Layout Refactor

- [x] 1.1 Refactor `PhotoList.vue`: remove left/right split layout, switch to full-width top-to-bottom layout
- [x] 1.2 Move group selector from `GroupList.vue` sidebar to top horizontal bar in `PhotoList.vue`
- [x] 1.3 Group selector: horizontal tabs with active state, overflow to dropdown when too many groups
- [x] 1.4 Add group management button (new/edit/delete) in top bar
- [x] 1.5 Integrate toolbar row: search | view toggle | add dropdown | batch operations

## 2. Grid Card Redesign

- [x] 2.1 Rewrite photo grid cards using pure TailwindCSS (remove `VCard`)
- [x] 2.2 Card design: rounded corners, aspect-ratio consistent, image dominant
- [x] 2.3 Photo name as overlay at bottom of image (semi-transparent bg)
- [x] 2.4 Hover effect: subtle scale + shadow
- [x] 2.5 Selection state: ring highlight or corner checkmark badge
- [x] 2.6 Deletion state: red ring + "删除中" overlay
- [x] 2.7 Tags display (up to 2-3 mini badges on card)

## 3. List View Adaptation

- [x] 3.1 Ensure list view table works in new full-width layout
- [x] 3.2 Inline editing still functional in list view

## 4. Existing Features Integration

- [x] 4.1 Drag-and-drop upload zone integrated into new layout
- [x] 4.2 Batch operations toolbar works in new layout
- [x] 4.3 Pagination footer at bottom
- [x] 4.4 Empty states styled for new layout
- [x] 4.5 Search input in top toolbar

## 5. Cleanup

- [x] 5.1 Remove or simplify `GroupList.vue` (or inline it into `PhotoList.vue`)
- [x] 5.2 Verify all existing functionality preserved
- [x] 5.3 Run frontend type-check and build
