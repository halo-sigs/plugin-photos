## Context

The photo management console is a Vue 3 admin surface built around `PhotoList.vue` and smaller components for grid, table, upload, and editing workflows. Recent changes added richer capabilities such as upload modal, tag filtering, batch operations, list mode, and drag sorting; the remaining problem is that those capabilities do not yet feel visually integrated. The implementation should stay within Halo component conventions, existing `@halo-dev/components`, existing FormKit usage, and UnoCSS utilities with the `:uno:` prefix.

## Goals / Non-Goals

**Goals:**
- Make the photo console feel like one coherent admin tool across toolbar, group navigation, grid, table, batch controls, and modals.
- Improve scanability and density without hiding existing actions.
- Stabilize layout dimensions so selection, loading, empty, drag, and inline-edit states do not cause avoidable shifts.
- Keep the implementation component-scoped and compatible with existing Halo console APIs.
- Verify the main flows at desktop and narrow viewport sizes.

**Non-Goals:**
- No backend API changes.
- No data model, permission, or route changes.
- No new UI library or design token system.
- No theme-side gallery rendering changes.
- No redesign of unrelated Halo console pages.

## Decisions

- **Refine in existing component boundaries.** Keep `PhotoList.vue` responsible for orchestration and state, while improving `PhotoGrid.vue`, `PhotoTable.vue`, and modal components in place. This avoids introducing a new layout abstraction before the current UI proves it needs one.
- **Use Halo components for commands and feedback.** Prefer `VButton`, `VDropdown`, `VSpace`, `VEmpty`, `VLoading`, `VStatusDot`, modal actions, and FormKit controls over custom button-like elements. This keeps focus, disabled, loading, and permission behavior closer to Halo console expectations.
- **Create stable UI regions.** Treat the header toolbar, group strip, batch toolbar, content area, and pagination as predictable regions with fixed minimum heights or reserved space where needed. Selection and loading states should update content inside those regions instead of resizing the whole page.
- **Make grid and list equal first-class views.** The grid should remain image-forward for visual inspection, while the list should be optimized for scanning metadata and inline edits. Both views must share selection semantics, deletion feedback, empty states, and loading behavior.
- **Keep visual treatments restrained.** Use neutral surfaces, borders, typography, and a small number of status colors. Avoid decorative gradients or large card treatments that would make the plugin feel separate from the Halo console.
- **Verify by interaction, not static inspection only.** The implementation should exercise group selection, search/tag filter, view switch, batch select, drag sort, add/upload, edit modal, empty state, and narrow viewport behavior before the change is considered done.

## Risks / Trade-offs

- [Risk] Visual polish could accidentally change behavior in batch selection, drag sorting, or inline editing.
  -> Mitigation: Keep state logic changes minimal and verify each interactive flow after UI edits.
- [Risk] Making the UI denser could reduce touch usability or mobile readability.
  -> Mitigation: Use responsive wrapping, minimum tap targets, and narrow viewport checks.
- [Risk] Over-normalizing component styles could fight Halo component defaults.
  -> Mitigation: Prefer local layout utilities around Halo components instead of deeply overriding component internals.
- [Risk] The current `PhotoList.vue` is already large, so more template edits may increase maintenance cost.
  -> Mitigation: Move purely presentational repeated structures into existing child components when it reduces complexity without changing behavior.
