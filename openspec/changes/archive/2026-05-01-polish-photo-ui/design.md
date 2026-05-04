## Context

The photo management console (`PhotoList.vue`) has grown organically and now suffers from a collection of small UI inconsistencies: button icons placed as siblings rather than in slots, group pills that feel cramped, a batch toolbar that causes layout jumps, persistent selection rings after modal close, redundant drag-upload strips, and a basic list table. These add up to a user experience that feels unpolished.

## Goals / Non-Goals

**Goals:**
- Unify all button icon placement using `VButton` icon slots
- Polish group selector bar spacing, hover/active states
- Redesign batch selection toolbar to be compact and stable-height
- Replace custom selection ring with a cleaner indicator
- Clear photo selection when edit modal closes
- Remove inline drag-upload strips; centralize upload through a dedicated modal
- Add `UppyUpload`-based upload modal with group selection
- Improve list view table styling
- Add tag-based photo filtering
- Fix edit modal image preview layout

**Non-Goals:**
- No theme-side changes
- No backend API changes (tag filter uses existing `name` query param on tags endpoint or keyword search)
- No data model changes

## Decisions

- **Use `VButton` icon slot**: Halo's `VButton` supports a default slot for icons. Passing icon components there ensures consistent spacing and hover behavior, rather than manually adding margin classes to inline icons.
- **Replace drag-upload strip with upload modal**: The inline strips take up vertical space and compete with the "新增" button. A dedicated `UppyUpload` modal is cleaner and supports drag-drop within the modal itself.
- **Tag filter uses existing keyword search or tags API**: The `photos` endpoint already supports `keyword` param. Tag filtering can use the tags endpoint combined with client-side filtering, or the backend `keyword` param if it searches tags. We'll check actual behavior during implementation.
- **Clear selection on modal close**: Set `selectedPhoto.value = undefined` in `onEditingModalClose` to remove the persistent ring.

## Risks / Trade-offs

- [Risk] Removing drag-upload strips may surprise users who were used to dragging directly onto the gallery.
  → Mitigation: The upload modal also supports drag-and-drop, so the capability is preserved in a more intentional location.
- [Risk] UppyUpload may require additional dependencies or configuration.
  → Mitigation: Halo's `@halo-dev/components` already bundles UppyUpload; check its props before implementation.
