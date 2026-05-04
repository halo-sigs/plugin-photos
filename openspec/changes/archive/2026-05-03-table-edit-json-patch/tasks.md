## 1. Refactor useInlineEdit.ts

- [x] 1.1 Import `ReplaceOperation` and `JsonPatchInner` from the generated API client
- [x] 1.2 Replace `updatePhoto` call with `patchPhoto` using `ReplaceOperation` ops for `displayName`, `groupName`, and `tags`
- [x] 1.3 Remove `cloneDeep` import from `es-toolkit` and the full-object mutation logic
- [x] 1.4 Build correct JSON Patch paths: `/spec/displayName`, `/spec/groupName`, `/spec/tags`

## 2. Verify

- [x] 2.1 Run `cd console && pnpm type-check` to confirm TypeScript compiles
- [x] 2.2 Open console and test inline editing of name, group, and tags in table mode
- [x] 2.3 Confirm save, cancel, and blur behaviors remain unchanged
