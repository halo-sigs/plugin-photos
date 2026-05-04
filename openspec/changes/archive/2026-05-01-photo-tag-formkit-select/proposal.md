## Why

The current Photo editing modal uses a custom tag input component that lacks key UX features: it doesn't suggest existing tags as the user types, and it can't create new tags on the fly. Switching to FormKit's `type="select"` with `tags` option and remote loading will provide a polished, consistent experience aligned with Halo's form system conventions.

## What Changes

- Replace the custom tag input in `PhotoEditingModal.vue` with a FormKit `select` field configured for tags
- Enable remote loading of existing tags from the photo API for auto-suggestions
- Allow users to create new tags inline (combobox-style)
- Reference: [Halo Form Schema docs](https://raw.githubusercontent.com/halo-dev/docs/refs/heads/main/versioned_docs/version-2.24/developer-guide/form-schema.md)

## Capabilities

### New Capabilities
- `photo-tag-input`: Tag input for photo editing with remote suggestions and inline creation using FormKit select

### Modified Capabilities
<!-- No existing specs to modify -->

## Impact

- Frontend: `console/src/components/PhotoEditingModal.vue`
- May require minor adjustments to the `Photo` type definition for tag handling
- No backend changes expected if tags are already stored as a string array
