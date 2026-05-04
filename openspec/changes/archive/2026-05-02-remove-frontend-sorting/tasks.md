## 1. Remove drag-and-drop from photo grid view

- [x] 1.1 Replace VueDraggable wrapper with plain div grid in PhotoGrid.vue
- [x] 1.2 Remove vue-draggable-plus import from PhotoGrid.vue
- [x] 1.3 Remove update:photos emit and localPhotos computed from PhotoGrid.vue

## 2. Remove drag-and-drop from photo list view

- [x] 2.1 Replace VueDraggable wrapper with plain tbody in PhotoTable.vue
- [x] 2.2 Remove vue-draggable-plus import from PhotoTable.vue
- [x] 2.3 Remove update:photos emit and localPhotos computed from PhotoTable.vue

## 3. Remove sorting logic and group drag-and-drop from PhotoList.vue

- [x] 3.1 Remove VueDraggable import from PhotoList.vue
- [x] 3.2 Remove handlePhotosUpdate and handleSavePhotoOrder functions
- [x] 3.3 Remove handleSaveGroupOrder function
- [x] 3.4 Remove priority sorting from photos queryFn
- [x] 3.5 Remove priority sorting from groups queryFn
- [x] 3.6 Replace VueDraggable sidebar wrapper with plain div loop
- [x] 3.7 Remove drag-handle icon from group buttons
- [x] 3.8 Remove @update:photos bindings from PhotoGrid and PhotoTable usage

## 4. Verify

- [x] 4.1 Run pnpm type-check in console/
- [x] 4.2 Run pnpm build in console/
