## Context

The console "上传图片" modal (`PhotoUploadModal.vue`) lets the user pick a target group from a `<FormKit type="select">` and uploads files via Halo's `UppyUpload` wrapper. Today the resulting photo is always created without a group, regardless of what the user picked.

Investigation traced the bug to a single client/server contract mismatch on the `group` parameter of `POST /apis/console.api.photo.halo.run/v1alpha1/photos/upload`:

- The Vue modal passes the value through Uppy's `:meta="{ group: uploadGroup }"`. With `@uppy/xhr-upload` (the transport Halo's `UppyUpload` wrapper uses), `meta` entries become **multipart/form-data fields** sitting alongside `file` in the request body. They are NOT promoted to the URL.
- The Java handler `PhotoEndpoint.uploadPhoto` reads `var group = request.queryParam("group").orElse("")` — i.e. only from the URL query string. The multipart field is parsed (`request.multipartData()`), but `parts.getFirst("group")` is never inspected.
- The OpenAPI declaration for the route at `PhotoEndpoint.java:64-73` even pins `group` as `ParameterIn.QUERY`, so the auto-generated TypeScript SDK (`console/src/api/generated/.../console-api-photo-halo-run-v1alpha1-photo-api.ts:164-188`) also expects it on the URL — but the actual upload UI bypasses the SDK and goes through `UppyUpload`, so SDK-vs-UI never aligned.

Net effect: `group` is always `""` on the server, `Photo.spec.groupName` is always empty, and "上传到分组" silently turns into "上传到未分组".

The wiring downstream is correct — `PhotoUploadServiceImpl.createPhoto` does set `spec.setGroupName(groupName)` from its parameter — so this is purely a parameter-transport problem at the endpoint boundary.

## Goals / Non-Goals

**Goals:**
- Make the upload endpoint honor the `group` value the console modal sends, so a chosen group ends up on `Photo.spec.groupName`.
- Pick one canonical wire format for `group` on this endpoint and document it consistently in OpenAPI so the regenerated SDK matches the server.
- Preserve the existing "no group selected → ungrouped photo" behavior described by the existing `photo-grouping` requirement.
- Keep the change surgical: one endpoint, one parameter, no broader refactor of the upload service or modal.

**Non-Goals:**
- Restructuring the upload pipeline (chunked uploads, progress, resumability) — out of scope.
- Changing how the standard CRUD path (`/apis/core.halo.run/v1alpha1/photos`) handles group assignment — that path already works correctly via `spec.groupName`.
- Adding new fields to the upload payload (e.g. tags, description). This change is scoped strictly to the `group` field.
- Migrating already-uploaded ungrouped photos. Photos that were silently misassigned by the bug remain ungrouped after the fix; users can re-assign them via the existing inline group editor.

## Decisions

### Decision 1: Read `group` from the multipart body on the server (canonical), keep query string as a fallback

The server `uploadPhoto` handler will read the `group` value from the parsed multipart parts (`parts.getFirst("group")` returning a `FormFieldPart`), falling back to `request.queryParam("group")` only when the multipart field is absent.

**Why multipart-first:**
- The endpoint is `multipart/form-data` by content type. Putting metadata in the same body as `file` is the natural shape and matches how every off-the-shelf uploader (Uppy, Dropzone, plain `<form enctype="multipart/form-data">`) sends extra fields.
- The console modal already sends it that way through `UppyUpload`'s `:meta` prop — no client change needed for the primary path.
- Avoids forcing the frontend to maintain a reactive URL string that mutates whenever the dropdown changes; reactive URL bindings into Uppy are easy to get subtly wrong.

**Why keep query as a fallback:**
- The current OpenAPI declaration says `ParameterIn.QUERY`, so the regenerated TypeScript SDK and any external script using `axios.post('/photos/upload?group=...')` already work that way. Treating query as a fallback preserves backward compatibility for those callers.
- It also keeps a trivial `curl -F file=@x.jpg "...upload?group=foo"` style of test harness working.

**Alternatives considered:**
- *Client-side only fix — append `?group=` to the Uppy endpoint URL.* Rejected because the OpenAPI shape is awkward (mixing a multipart body with a query metadata field is a smell), and we'd need to make the `endpoint` prop reactive so it picks up dropdown changes after Uppy is initialised. Easy to break.
- *Multipart only, drop query support entirely.* Rejected because it silently breaks the existing generated SDK signature and any external caller that already invokes the published endpoint shape. The fallback is a few extra lines and zero ambiguity.

### Decision 2: Update the OpenAPI declaration to advertise `group` as a multipart form field

The route declaration at `PhotoEndpoint.java:64-73` will be revised so OpenAPI describes the upload request as `multipart/form-data` with two fields, `file` (binary) and `group` (string, optional). The query-parameter declaration is dropped.

**Why:**
- The server's canonical input becomes the body field; the SDK should reflect that so the generated client doesn't drift from the server again.
- A single source of truth for the wire shape means the next contributor can't repeat the original mistake.
- Springdoc supports `@RequestBody`-style multipart definitions in `RouterFunctionBuilder` via `requestBody(...)` with a `Content` of `multipart/form-data`.

**Trade-off:** Existing generated SDK signature `uploadPhoto(group?: string)` will become `uploadPhoto({ file: File, group?: string })` (exact shape depends on Halo's generator template). Since the only in-tree caller of this endpoint is `PhotoUploadModal.vue` and it bypasses the SDK by handing the URL to Uppy directly, the regeneration has no in-tree breakage. External plugin authors who imported the generated client will need to adjust — acceptable for a bug-fix release because the previous signature didn't actually work for the realistic case.

### Decision 3: Empty/missing `group` keeps producing ungrouped photos

If neither the multipart field nor the query parameter is present, or both are empty strings, the handler treats the upload as ungrouped (current behavior). No defaulting, no implicit "uncategorized" group.

**Why:** The existing `photo-grouping` capability already specifies "Console Upload Without Group" — the empty case is intentional, not a leftover bug. Preserving it keeps this change a strict bug fix rather than a behavior change.

### Decision 4: Frontend remains the source of the chosen group; modal does not re-send via the standard CRUD path

`PhotoUploadModal.vue` keeps its current `:meta="{ group: uploadGroup }"` wiring. We do not switch the modal to first upload a file via attachment APIs and then `createPhoto` against `/apis/core.halo.run/v1alpha1/photos`.

**Why:** The custom upload endpoint exists precisely to bundle attachment creation, EXIF parsing, and Photo creation into a single call (see `PhotoUploadServiceImpl`). Splitting it would regress upload UX (two round-trips, worse failure semantics) for no gain — the bug is one parameter, not a missing path.

## Risks / Trade-offs

- **[Risk] External plugin authors who imported the regenerated SDK will see a signature change for `uploadPhoto`.** → Mitigation: Document the change in the change archive notes; the previous query-only signature was effectively broken for the in-modal flow, so most callers were either using the URL directly (continues to work via fallback) or hitting an unusable code path.
- **[Risk] `parts.getFirst("group")` returns `Part`, which for form fields is `FormFieldPart` carrying the value via `value()`. Casting wrong (e.g. casting to `FilePart`) would crash.** → Mitigation: Type-check the cast and only read `value()` when the part is a `FormFieldPart`; otherwise fall back to query.
- **[Risk] An attacker might attempt to inject a `group` value targeting a non-existent group.** → Mitigation: Behavior is unchanged from the current direct CRUD path — if the group does not exist, the photo is still created with that `groupName` value (dangling reference), matching how `PhotoCreationModal` already works. This change does not introduce new validation; if validation is desired, it belongs in a separate change.
- **[Trade-off] Two transports for the same field (multipart preferred, query fallback) is mildly more code than one.** → Acceptable: the fallback is a one-liner and explicitly preserves backward compatibility with the previously documented contract.
- **[Risk] `PhotoUploadModal.vue:15` initialises `uploadGroup` from `props.defaultGroup` without a watcher.** → Out of scope for this fix (the parent passes the value synchronously today and the bug under investigation is independent), but flagged for follow-up if late-arriving `defaultGroup` is ever needed.

## Migration Plan

1. Land the server change (multipart-first read, query fallback, OpenAPI update) and regenerate the TS SDK via `./gradlew generateApiClient`.
2. Verify the modal still works end-to-end against a fresh build; no client code changes are strictly required, but commit the regenerated SDK.
3. Roll out as a normal plugin release. No data migration is needed — pre-existing photos that were silently ungrouped retain `spec.groupName == ""` and can be re-assigned by the user through the inline group editor.

**Rollback:** Revert the endpoint change. The bug returns to its previous state (selected group ignored), but no data is corrupted because the server only ever wrote empty strings under the broken behavior.

## Open Questions

- Should we also add server-side validation that the supplied `group` references an existing `PhotoGroup`? Out of scope for this fix; flag for a future hardening change.
