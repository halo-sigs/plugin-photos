## 1. UI Components

- [x] 1.1 Create `ExternalLinkImportModal.vue` component with group selector and URL textarea
- [x] 1.2 Add "批量添加外链" option to the "新增" dropdown in `AddButton.vue`
- [x] 1.3 Wire the dropdown option to open `ExternalLinkImportModal.vue`
- [x] 1.4 Add i18n strings for the new UI labels (modal title, dropdown option, form fields) — project uses hardcoded Chinese strings throughout, no i18n setup

## 2. Batch Import Logic

- [x] 2.1 Implement URL parsing logic: split by newline, trim whitespace, filter empty lines
- [x] 2.2 Implement name derivation from URL pathname (last segment, fallback to "未命名")
- [x] 2.3 Integrate with existing Photo create API to batch-create photos sequentially or with limited concurrency (chunk size 5, Promise.all within chunk)
- [x] 2.4 Handle API errors gracefully: try/catch around entire batch, shows error toast on failure
- [x] 2.5 Show success notification with created count after import completes
- [x] 2.6 Refresh the photo list after successful import (invalidates QK_PHOTOS, QK_PHOTO_GROUPS, QK_PHOTO_TAGS)

## 3. Testing & Verification

- [x] 3.1 Manually test the modal with a list of valid URLs
- [x] 3.2 Test with empty lines, whitespace-only lines, and mixed input
- [x] 3.3 Test group assignment (both grouped and ungrouped)
- [x] 3.4 Run `pnpm lint` and `pnpm type-check` in the `console` directory
- [x] 3.5 Build the plugin with `./gradlew build` to verify no compile errors
