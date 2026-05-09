## Context

The plugin recently gained EXIF extraction during upload (`PhotoUploadServiceImpl`), but all photos uploaded before that feature have `exif: null`. Users need a way to retroactively populate EXIF for existing photos without re-uploading.

The attachment storage model is well understood:
- `photo.spec.url` stores the Attachment's `status.permalink` (e.g., `/upload/IMG_1492.jpeg`)
- Local attachments live under `{halo.workdir}/attachments/` with a path matching the permalink
- `BackupRootGetter.get().getParent()` yields `{halo.workdir}`

The frontend already has a mature batch-operation framework (`useBatchOperations` with `runWithConcurrency`).

## Goals / Non-Goals

**Goals:**
- Allow users to select any number of existing photos and re-read their EXIF in one action
- Reuse the same EXIF parsing logic that runs at upload time (single source of truth)
- Keep the implementation consistent with existing batch operation patterns (delete, move group, add tags)

**Non-Goals:**
- Supporting external/non-local attachments (S3, OSS, etc.) — out of scope; silently skipped
- Supporting photos created with manually-entered URLs (no Attachment record) — silently skipped
- Changing the EXIF data model or adding new EXIF fields
- Adding a theme-side or public API for re-extraction (console-only)

## Decisions

### 1. Single-item API, client-side batching
**Decision**: `POST /photos/{name}/reextract-exif` per photo, frontend loops with `runWithConcurrency(..., 5)`.

**Rationale**: This is identical to how batch delete, batch move, and batch add-tags already work. A bulk endpoint would require new frontend orchestration and backend batch-abstraction code for marginal gain. The existing pattern provides per-item progress feedback naturally.

### 2. Extract EXIF parsing into a shared component
**Decision**: Move `PhotoUploadServiceImpl.extractExif(byte[])` and the `ExifData` inner class into a standalone `@Component` (e.g., `ExifExtractor`).

**Rationale**: Both upload and re-extraction need identical parsing. Keeping it private inside `PhotoUploadServiceImpl` forces duplication. A shared component is trivial to unit-test in isolation.

**Alternative considered**: Make the methods package-private and call across classes. Rejected — a dedicated component is cleaner and more testable.

### 3. File resolution via `BackupRootGetter` + permalink
**Decision**: Resolve the local path as `backupRootGetter.get().getParent().resolve("attachments" + permalink)`.

**Rationale**: The user confirmed `BackupRootGetter` is injectable and the mapping from permalink to local path is direct. No need to query Attachment metadata for a file path — the permalink itself encodes the relative path.

**Risk**: If Halo changes the local attachment path layout in the future, this resolution breaks. The mitigation is that this is a console-only maintenance operation, not a critical data path.

### 4. Skip (no error) for missing/unreadable files
**Decision**: Return the unmodified Photo when the attachment file cannot be resolved or read.

**Rationale**: In a batch of 50 photos, 3 might be external URLs or missing files. Failing the entire batch because of edge cases would be a poor UX. Silent skip with frontend-level "X of Y succeeded" feedback is the pragmatic choice.

## Risks / Trade-offs

| Risk | Mitigation |
|------|-----------|
| Permalink-to-path mapping changes in future Halo versions | This is a maintenance operation, not critical path; can be adapted if needed |
| Reading large files into memory for EXIF parsing | Same constraint as upload flow (50MB max); the existing `extractExif(byte[])` already loads full file |
| Concurrent batch calls hitting disk I/O limits | `runWithConcurrency(..., 5)` caps parallelism; acceptable for a maintenance operation |
| GPS coordinates written to console-visible `Photo.exif` but hidden in `PhotoVo` | Existing behavior unchanged; `PhotoVo.from()` already nulls GPS fields |

## Migration Plan

No migration needed. This is a purely additive feature:
- Existing photos keep their current `exif` value (null or populated)
- Existing APIs are unchanged
- New endpoint is additive under `console.api.photo.halo.run`

## Open Questions

*(none — all key decisions resolved during exploration)*
