## Context

The photo editing modal (`PhotoEditingModal.vue`) currently uses a custom tag input component. Halo's form system is built on FormKit, which provides a native `select` input with `tags` support that can load options remotely and allow inline creation. Using the built-in FormKit component aligns with Halo's design system and reduces custom code.

## Goals / Non-Goals

**Goals:**
- Replace the custom tag input with FormKit `type="select"` (tags mode) in `PhotoEditingModal.vue`
- Support remote loading of existing tags for auto-suggestions
- Support creating new tags inline
- Maintain the existing data model (tags stored as string array)

**Non-Goals:**
- No backend API changes
- No changes to theme-side rendering
- No changes to tag storage format

## Decisions

- **Use FormKit select with tags**: Halo already depends on FormKit via `@halo-dev/components`. Using the native component ensures consistency with other forms in the console and reduces maintenance burden.
- **Remote load from photo list endpoint**: Existing tags can be extracted from all photos' `spec.tags` fields. The console already fetches photos via `@tanstack/vue-query`; we can derive unique tags from that data rather than adding a new API endpoint.

## Risks / Trade-offs

- [Risk] FormKit select tags behavior may differ slightly from the current custom component (e.g., keyboard navigation, delimiters).
  → Mitigation: Test the interaction manually after implementation.
- [Risk] Loading all photos to extract tags may not scale with very large galleries.
  → Mitigation: The existing photo list is already fetched for the gallery view; tag extraction is a cheap client-side computation.
