## Context

The `useInlineEdit` composable currently sends a full `Photo` object via `photosCoreApiClient.photo.updatePhoto()` every time an inline cell is edited. This works, but:

1. The request payload is large relative to the change.
2. Concurrent edits to different fields can overwrite each other.
3. The generated API client already provides `patchPhoto(name, jsonPatchInner)` which sends `application/json-patch+json`.

This is a targeted frontend refactor with no backend changes.

## Goals / Non-Goals

**Goals:**
- Replace full PUT with JSON Patch (`patchPhoto`) for inline edits of `displayName`, `groupName`, and `tags`.
- Keep the same debounce (`300ms`), error handling (`Toast.error`), and save/cancel UX.
- Remove `es-toolkit` `cloneDeep` dependency from `useInlineEdit.ts`.

**Non-Goals:**
- Changing any other update flow (e.g., modal-based full edit still uses PUT).
- Adding new user-facing capabilities or UI changes.
- Modifying backend endpoints or DTOs.

## Decisions

### Use `ReplaceOperation` for all patches
- `displayName` and `groupName` are scalar string values. A single `replace` op targeting the field path is sufficient.
- `tags` is an array. For consistency and correctness, replacing the whole array (`/spec/tags`) is simpler than computing per-tag diff operations. The `tags` array is small (< 10 items) so the payload difference is negligible.

### Remove `cloneDeep(photo)` and full-object reassignment
- The old flow cloned the entire `Photo`, mutated one field, and PUT the whole object.
- The new flow constructs only the patch operations needed.
- This eliminates the `cloneDeep` import from `es-toolkit` in this file.

### Keep `setTimeout` debounce
- The existing `300ms` debounce prevents race conditions from rapid `blur` + `enter`.
- JSON Patch is still idempotent, but the debounce protects the UI state and avoids unnecessary requests.

### Path format
- `displayName` → `/spec/displayName`
- `groupName` → `/spec/groupName`
- `tags` → `/spec/tags`

## Risks / Trade-offs

- **[Risk]** If a field is unset on the server, `replace` on that path may fail if the JSON Patch engine expects `add` instead.
  - **Mitigation**: Halo's Kubernetes-style Extension controller accepts both `replace` and `add` for field updates. The generated `ReplaceOperation` type is the correct abstraction here.

- **[Risk]** Typing mismatch between `JsonPatchInner` types and manual object construction.
  - **Mitigation**: Import `ReplaceOperation` from the generated client and type-annotate the patch array as `Array<JsonPatchInner>`.
