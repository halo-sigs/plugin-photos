## 1. Prepare Tag Data Source

- [x] 1.1 Read `PhotoEditingModal.vue` to understand current tag input implementation
- [x] 1.2 Identify where existing photo data is available (via props or shared query) to extract unique tags
- [x] 1.3 Add computed property or helper to derive unique tag list from existing photos

## 2. Replace Tag Input with FormKit Select

- [x] 2.1 Replace custom tag input component in `PhotoEditingModal.vue` with FormKit `type="select"` configured for tags
- [x] 2.2 Bind tag options to the derived unique tag list
- [x] 2.3 Enable inline tag creation via FormKit select props
- [x] 2.4 Ensure two-way binding with photo's `spec.tags` array

## 3. Verify and Test

- [x] 3.1 Run `pnpm type-check` to ensure TypeScript compiles (passes for PhotoEditingModal.vue; pre-existing error in PhotoList.vue only)
- [x] 3.2 Run `pnpm lint` to ensure no style issues (pre-existing prettier plugin compatibility issue)
- [x] 3.3 Open console at `http://127.0.0.1:8090/console`, login, and test tag input in photo editing modal
- [x] 3.4 Verify existing tags are suggested when typing
- [x] 3.5 Verify new tags can be created inline
