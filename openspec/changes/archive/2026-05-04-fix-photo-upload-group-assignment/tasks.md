## 1. Backend: Honor `group` from multipart body

- [x] 1.1 In `src/main/java/run/halo/photos/PhotoEndpoint.java`, update `uploadPhoto(ServerRequest)` so it resolves `group` from the parsed multipart parts first (look up the `group` part, cast to `FormFieldPart`, read `value()`) and only falls back to `request.queryParam("group")` when the multipart field is absent or blank
- [x] 1.2 Treat an empty/whitespace `group` from either source as "no group selected" — pass an empty string into `photoUploadService.upload(filePart, group)` so the existing ungrouped behavior is preserved
- [x] 1.3 Guard the cast: if the looked-up part exists but is not a `FormFieldPart` (e.g. someone uploads a binary named `group`), ignore it and fall through to the query parameter

## 2. Backend: OpenAPI declaration

- [x] 2.1 In the same `endpoint()` builder, replace the `ParameterIn.QUERY` declaration for `group` with a `requestBody(...)` of `multipart/form-data` describing two fields: `file` (binary, required) and `group` (string, optional)
- [x] 2.2 Keep the route's `produces`/response declaration unchanged (still returns the created `Photo`)
- [x] 2.3 Optional: add a short note in the operation `description` clarifying that `group` MAY also be provided as a query parameter for backward compatibility

## 3. Regenerate the TypeScript SDK

- [x] 3.1 Run `./gradlew generateApiClient` to regenerate the client under `console/src/api/generated/`
- [x] 3.2 Inspect `console/src/api/generated/api/console-api-photo-halo-run-v1alpha1-photo-api.ts` and confirm `uploadPhoto` no longer receives `group` as a query parameter (it should now appear as a form field on the request body, per the new OpenAPI shape)
- [x] 3.3 Commit the regenerated files; do not hand-edit them

## 4. Frontend: confirm modal behavior is unchanged

- [x] 4.1 Re-read `console/src/components/PhotoUploadModal.vue` and confirm the existing `:meta="{ group: uploadGroup }"` wiring on `<UppyUpload>` is sufficient — no client change should be needed because Uppy already submits `meta` as multipart form fields
- [x] 4.2 If the regenerated SDK breaks any other in-tree caller of `uploadPhoto` (search the console for `uploadPhoto(` references), update those callers to the new signature

## 5. Verification

- [x] 5.1 Build: `./gradlew build` — ensure both Java compile and frontend bundle succeed
- [x] 5.2 Start the dev server: `./gradlew haloServer`, open `http://127.0.0.1:8090/console`, login as `admin` / `admin`
- [x] 5.3 Manual scenario A — Upload with a non-empty group: navigate to 图库, open the upload modal, select a real group in "上传到分组", upload one image, then confirm the new photo appears under that group in the sidebar AND that its `spec.groupName` matches the chosen group's `metadata.name` (inspect via the photo edit modal or the `/apis/core.halo.run/v1alpha1/photos` endpoint)
- [x] 5.4 Manual scenario B — Upload with the "未分组" option: open the upload modal, leave the selector on "未分组", upload one image, confirm the photo lands in the ungrouped view and its `spec.groupName` is empty/unset
- [x] 5.5 Manual scenario C — Direct API smoke (optional but recommended): `curl -F file=@sample.jpg -F group=<existing-group> "<console-api>/photos/upload"` and confirm the returned `Photo` has `spec.groupName` equal to `<existing-group>`
- [x] 5.6 Manual scenario D — Backward compatibility: `curl -F file=@sample.jpg "<console-api>/photos/upload?group=<existing-group>"` (no multipart `group` field) and confirm the returned `Photo` still picks up `<existing-group>` from the query parameter

## 6. Wrap-up

- [x] 6.1 Update or add a brief note in the change archive when this change is archived, mentioning that the upload endpoint's wire shape for `group` was canonicalized to a multipart field
- [x] 6.2 If any external integrations are tracked in the project, flag the SDK signature change so downstream plugin authors are aware
