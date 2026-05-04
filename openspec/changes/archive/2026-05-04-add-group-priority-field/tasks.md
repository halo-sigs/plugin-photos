## 1. Frontend Types & Form

- [x] 1.1 Add `priority: number` to `PhotoGroupFormState` in `console/src/types/index.ts`
- [x] 1.2 Add `priority` number input to `GroupForm.vue` (label: "权重", help: "数字越大越靠前", default 0)
- [x] 1.3 Update `GroupForm.vue` `onSubmit` to emit `priority` alongside `displayName` and `annotations`

## 2. Frontend Modals

- [x] 2.1 Update `GroupCreationModal.vue` to pass `data.priority` to the create payload instead of hard-coding `0`
- [x] 2.2 Update `GroupEditingModal.vue` to include `{ op: "add", path: "/spec/priority", value: data.priority }` in the JSON Patch and pass `priority` into `GroupForm` initial state

## 3. Backend Sorting

- [x] 3.1 Update `PhotoGroupServiceImpl.listPhotoGroup()` to apply explicit sort: `spec.priority` DESC, `metadata.creationTimestamp` DESC, `metadata.name` ASC
- [x] 3.2 Verify `PhotoGroupEndpoint` does not override the sort (it delegates to service)

## 4. API Client & Verification

- [x] 4.1 Run `./gradlew generateApiClient` to regenerate the TypeScript API client if needed
- [x] 4.2 Run `./gradlew build` to verify compilation
- [x] 4.3 Start dev server (`./gradlew haloServer`), open console, create/edit groups with different priorities, and confirm group filter bar order reflects DESC sort
