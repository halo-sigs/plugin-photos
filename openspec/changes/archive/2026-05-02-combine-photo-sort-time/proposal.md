## Why

The current shooting-time sort treats photos without `spec.dateTimeOriginal` as a separate tail group, so newly created or recently uploaded photos without EXIF time can appear permanently behind older photos that do have EXIF time. Users expect the photo list to sort by the best available photo time, using EXIF shooting time when present and creation time when EXIF time is missing.

## What Changes

- Change the default photo ordering to use an effective time: `spec.dateTimeOriginal` when present, otherwise `metadata.creationTimestamp`.
- Change explicit `sort=spec.dateTimeOriginal,asc|desc` behavior to sort all photos by that effective time instead of placing EXIF-empty photos at the end.
- Keep explicit `sort=metadata.creationTimestamp,asc|desc` behavior unchanged.
- Apply the combined effective-time ordering consistently to console photo list responses and theme-side finder/default photo ordering.
- Preserve stable tie-breakers so ordering remains deterministic when photos share the same effective time.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `photo-sort-options`: Shooting-time sort now combines EXIF shooting time and creation time into one effective time sort key.
- `photo-grouping`: Theme finder default photo ordering uses the same effective time fallback semantics for grouped, ungrouped, and all-photo lists.

## Impact

- **Backend**: `PhotoQuery`, finder default sorting, and any shared sorting helper logic used by console/theme photo lists.
- **Frontend**: No new UI control is required; existing sort options keep the same labels and query values while backend semantics improve.
- **API contract**: Existing `sort` query parameter values remain compatible, but `spec.dateTimeOriginal` sorting changes behavior for photos without EXIF time.
- **Data**: No migration or schema change is required.
