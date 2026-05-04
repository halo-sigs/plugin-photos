## Context

The photo plugin currently supports adding photos via upload or manual single-entry creation. There is no way for users to batch-import multiple external image URLs at once. This is a common need when migrating image collections or aggregating images from external sources.

## Goals / Non-Goals

**Goals:**
- Add a "批量添加外链" option in the PhotoList "新增" dropdown
- Provide a modal with group selection and a textarea for URL lists
- Create one Photo per non-empty URL line, deriving the name from the URL
- Show progress/success feedback and skip invalid URLs gracefully

**Non-Goals:**
- No backend batch endpoint is required; frontend will call the existing single-create API sequentially
- No URL validation beyond basic non-empty checks (actual accessibility is not verified)
- No duplicate detection within the URL list

## Decisions

**Decision 1: Frontend-only batch creation (no new backend endpoint)**
- **Rationale**: The existing Photo CRUD endpoint at `/apis/core.halo.run/v1alpha1/photos` already supports creating a Photo with a POST. Sequential `Promise.all` from the frontend is sufficient for typical batch sizes (< 100). This avoids backend complexity and keeps the change minimal.
- **Alternative considered**: A dedicated `POST /apis/console.api.photo.halo.run/v1alpha1/photos/batch` endpoint. Rejected because it adds backend work for marginal gain at expected scale.

**Decision 2: Name derived from URL pathname**
- **Rationale**: Using the last segment of the URL path (filename) as the default `spec.name` is predictable and user-friendly. If the URL ends with `/` or has no filename, fall back to "未命名".
- **Alternative considered**: Prompt user for a naming template. Rejected to keep the modal simple.

**Decision 3: Reuse existing `PhotoEditingModal.vue` patterns**
- **Rationale**: Consistency with existing modals (form layout, validation, `@halo-dev/components` usage) reduces cognitive load and styling effort.

## Risks / Trade-offs

- **[Risk] Large URL lists could cause many sequential API calls, leading to rate-limiting or UI blocking**
  - **Mitigation**: Use `Promise.all` with a concurrency limit (e.g., 5 parallel requests) or sequential chained promises. Show a loading spinner during the operation.
- **[Risk] URLs with non-image content are not validated**
  - **Mitigation**: Accept any URL; the photo system already stores URLs as-is. Invalid images will fail at render time, which is consistent with manual entry behavior.
- **[Risk] Duplicate names if URLs have identical filenames**
  - **Mitigation**: Accept duplicates; Halo's metadata.name is unique, but spec.name is not required to be. The system will create distinct Photo resources.
