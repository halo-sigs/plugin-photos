## Why

The inline cell editing in the admin photo table currently sends a full PUT update (`updatePhoto`) even though only a single field changes. The generated API client already exposes a `patchPhoto` method that accepts RFC 6902 JSON Patch operations (`application/json-patch+json`). Using PATCH for partial updates is more efficient and avoids accidentally overwriting unrelated fields during concurrent edits.

## What Changes

- Refactor `useInlineEdit.ts` to persist inline edits via `photosCoreApiClient.photo.patchPhoto()` instead of `updatePhoto()`.
- Build JSON Patch operations targeting only the changed field (`/spec/displayName`, `/spec/groupName`, or `/spec/tags`).
- Remove the full `cloneDeep(photo)` and manual reassignment of fields.
- Keep the existing debounce, save/cancel/focus flow, and error handling unchanged.

## Capabilities

### New Capabilities

(none — this is an internal persistence optimization; user-facing behavior remains unchanged)

### Modified Capabilities

(none — requirements of existing capabilities are not changing)

## Impact

- **Frontend**: `console/src/composables/useInlineEdit.ts` only.
- **API**: Switches from `PUT /apis/core.halo.run/v1alpha1/photos/{name}` to `PATCH` with JSON Patch body.
- **No backend changes** — the endpoint already supports JSON Patch through Halo's Extension framework.
