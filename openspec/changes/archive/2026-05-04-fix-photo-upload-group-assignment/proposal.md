## Why

The console upload modal lets the user pick a target group from a dropdown, but uploaded photos are always created without a group. The selected group is silently dropped before the photo is persisted, so users have to re-assign every uploaded photo by hand. The break is a client/server contract mismatch on a single field, but it defeats the entire upload-into-a-group workflow.

## What Changes

- Fix the `POST /apis/console.api.photo.halo.run/v1alpha1/photos/upload` endpoint to honor the `group` value sent by the console upload modal so the resulting `Photo.spec.groupName` matches the user's selection.
- Align the client/server contract for the upload `group` parameter so the value travels reliably end-to-end (the modal sends it via Uppy `meta` as a multipart form field; the backend currently only reads `request.queryParam("group")`).
- Update the OpenAPI declaration of the upload endpoint so the regenerated TypeScript SDK and external API consumers agree with the server on where `group` is read from.
- Keep the existing "upload without selecting a group" behavior working — an unset/empty value MUST continue to produce an ungrouped photo.

## Capabilities

### New Capabilities
<!-- None — this is a bug fix that tightens existing photo-grouping behavior. -->

### Modified Capabilities
- `photo-grouping`: Strengthen the upload requirement so that when the user DOES select a group, the resulting photo is created in that group. The existing "Console Upload Without Group" requirement only covers the empty-group case; the bug lives in the with-group path, which is currently unspecified.

## Impact

- **Backend**
  - `src/main/java/run/halo/photos/PhotoEndpoint.java` — `uploadPhoto` handler and the OpenAPI route declaration for `POST photos/upload`.
- **Frontend**
  - `console/src/components/PhotoUploadModal.vue` — the Uppy `endpoint` / `meta` wiring may need to change depending on which side of the contract is canonicalized (see `design.md`).
  - `console/src/api/generated/**` — must be regenerated via `./gradlew generateApiClient` after the OpenAPI spec change so the SDK matches the server.
- **API contract**
  - The upload endpoint's `group` parameter location (multipart form field vs. query string) is normalized. External callers that already invoke `POST /photos/upload?group=...` continue to work if we keep the query path as a fallback.
- **Verification**
  - Manual smoke test on `http://127.0.0.1:8090/console`: uploading with a selected group must produce photos whose `spec.groupName` equals that group; uploading with no group selected must continue to produce ungrouped photos.
