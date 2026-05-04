## Context

`Photo` currently has a single `spec` field that mixes two semantically distinct categories of data:

| Type | Example fields | Who sets it |
|------|----------------|-------------|
| Desired user state | displayName, url, groupName, tags | User manually |
| EXIF metadata | make, model, iso, gpsLatitude… | Auto-read at upload |

Additionally, `PhotoUploadServiceImpl` serializes the full raw EXIF payload to `metadata.annotations["photos.halo.run/exif"]`. Annotations are stored as key-value strings; large values bloat the Extension object size and degrade list performance, with most fields having no display value to the user.

## Goals / Non-Goals

**Goals:**
- Move all EXIF fields from `PhotoSpec` into a new top-level `PhotoExif` class
- Add missing common shooting-parameter fields: fNumber, exposureTime, focalLength, and more
- Remove the raw EXIF write to `metadata.annotations`
- Update `PhotoVo` to expose `exif` for theme consumption

**Non-Goals:**
- Database migration (feature not yet released; local dev resets by deleting the `workplace` directory)
- Indexing EXIF fields in the database (no filter-by-EXIF requirement at this time)
- Adding an EXIF editing UI in the console (fields will be in place for future extension)

## Decisions

### Decision 1: `exif` as a top-level field on `Photo`, not nested inside `spec`

**Choice:** Top-level field, sibling to `spec`.

**Rationale:**
- In the Kubernetes-style Extension model, `spec` semantically represents "desired user state". EXIF is system-read, not user-controlled.
- A top-level field yields shorter JSON access paths (`photo.exif.make` vs `photo.spec.exif.make`), cleaner for theme templates.
- Future permission controls or webhook policies on EXIF vs. user fields are easier to apply at the top level.

**Rejected alternative:** Nesting as `spec.exif` — inconsistent with Halo Extension conventions, no additional benefit.

---

### Decision 2: Categorical EXIF fields stored as Integer (EXIF standard codes)

**Choice:** flash, whiteBalance, exposureMode, exposureProgram, meteringMode stored as `Integer` raw EXIF enum values.

**Rationale:**
- Preserves structured information; the frontend can perform programmatic checks (e.g., "was flash fired?").
- EXIF standard enum values are stable; maintaining a frontend mapping table is low cost.
- Storing as String description (e.g., "Flash fired") makes filtering/comparison fragile.

**Rejected alternative:** String description — easier to display directly, but loses programmatic query capability.

---

### Decision 3: `exposureTime` stored as String

**Choice:** `exposureTime` is `String`, preserving fractional format (e.g., `"1/125"`).

**Rationale:**
- Shutter speed is conventionally displayed as a fraction. Storing as Double (0.008) requires the frontend to reconstruct the fraction, and floating-point precision can introduce formatting noise.
- If users manually edit the field, typing `"1/125"` is more intuitive than `0.008`.

---

### Decision 4: `imageWidth` / `imageHeight` moved into `exif`, removed from `spec`

**Rationale:** These are physical properties of the image file — auto-read, not user-controlled. Moving them into `exif` is consistent with the semantic boundary. `spec` retains only fields the user actively manages.

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Themes already referencing `spec.make` etc. will break after the field move | Feature is unreleased; no external theme dependencies. Docs will be updated at release. |
| Frontend generated code path changes after `generateApiClient`; stale references may be missed | TypeScript compilation will surface all incompatible references at build time. |
| Some images have no EXIF data; `photo.exif` will be null | Frontend and templates must null-check `exif`, consistent with existing nullable spec field handling. |
