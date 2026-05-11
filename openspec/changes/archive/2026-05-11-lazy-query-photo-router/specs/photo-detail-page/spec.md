## MODIFIED Requirements

### Requirement: Detail Template Receives A Stable Set Of Model Attributes

The detail route SHALL expose the following model attributes to the `photo` template. Reactive data sources (`neighbors`, `prev`, `next`, `position`, `total`, `title`) SHALL be wrapped in `org.thymeleaf.context.LazyContextVariable` so they are resolved only when the template references them.

- `photo`: the current `PhotoVo`
- `neighbors`: the list of `PhotoVo` in the sliding window, in sort order, including the current photo — resolved lazily
- `prev`: the `PhotoVo` immediately before the current photo in sort order, or absent/null when the current photo is the first — resolved lazily
- `next`: the `PhotoVo` immediately after the current photo in sort order, or absent/null when the current photo is the last — resolved lazily
- `position`: the 1-based index of the current photo within the filtered context — resolved lazily
- `total`: the total count of photos in the filtered context — resolved lazily
- `group`: the `group` query parameter value (or null if absent)
- `page`: the `page` query parameter value (defaulting to 1)
- `size`: the `size` query parameter value (defaulting to the configured page size)
- `backUrl`: a prebuilt URL string back to the originating list view, including any active `group`, `page`, and `size`
- `title`: the page title string (from `base.title` setting or the photo's `spec.displayName`) — resolved lazily
- `_templateId`: the string `"photo"`
- `photoUrl`: the per-request URL helper described in the `theme-url-context` capability

#### Scenario: Single photo detail template rendering

- **WHEN** the detail route renders `photo.html` for a known photo
- **THEN** the template can access `photo`, `neighbors`, `prev`, `next`, `position`, `total`, `group`, `page`, `size`, `backUrl`, `title`, `_templateId`, and `photoUrl` from the rendering context, with lazy variables resolving transparently on first access

#### Scenario: First photo has no previous neighbor

- **WHEN** the current photo is at index 0 of the filtered list
- **THEN** `prev` is null/absent and `next` is the photo at index 1

#### Scenario: Last photo has no next neighbor

- **WHEN** the current photo is at the last index of the filtered list
- **THEN** `next` is null/absent and `prev` is the photo immediately before

#### Scenario: SPA theme does not trigger neighbor queries

- **WHEN** the `photo.html` template does not reference `${neighbors}`, `${prev}`, `${next}`, `${position}`, `${total}`, or `${title}`
- **THEN** the underlying filtered photo list query does not execute
