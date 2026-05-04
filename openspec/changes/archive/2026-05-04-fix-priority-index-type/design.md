## Context

`PhotoPlugin` registers a single-value index on `PhotoGroup` for the `spec.priority` field. The index is declared with `String.class` and the index function converts the integer priority to a decimal string:

```java
IndexSpecs.<PhotoGroup, String>single("spec.priority", String.class)
    .indexFunc(group ->
        group.getSpec() == null || group.getSpec().getPriority() == null
            ? String.valueOf(0) : group.getSpec().getPriority().toString()
    )
```

Because `String` ordering is lexicographic, the index treats `"10"` as less than `"2"`, which is incorrect for numeric priority values. The bug is latent: today no code performs range queries or index-backed sorts on this field, so the incorrect ordering is not observable. However, `ReactiveExtensionClient` indexes are the intended fast path for exactly those operations.

Halo's `IndexSpecs.single` API is generic over `K extends Comparable<K>`. `Integer` satisfies this bound, so the framework natively supports integer-typed indexes.

## Goals / Non-Goals

**Goals:**
- Change the `spec.priority` index type from `String.class` to `Integer.class`.
- Update the index function to return `Integer` (or `0` for null) instead of converting to `String`.
- Confirm the plugin still compiles and tests pass after the change.

**Non-Goals:**
- Adding a `spec.priority` index to `Photo` (it currently has none; out of scope).
- Changing any query, service, or finder logic that consumes the index.
- Writing a migration to rebuild existing index data manually (Halo rebuilds indexes automatically on scheme re-registration).

## Decisions

### 1. Use `Integer.class` instead of zero-padded string formatting

An alternative fix is to keep `String.class` but pad the integer with leading zeros (e.g., `%05d`) so lexicographic order matches numeric order. Rejected because:

- **Integer.class is the semantically correct type.** The field is an `Integer`; the index should reflect that.
- **Padding introduces an implicit width limit.** `%05d` breaks for priorities > 99 999. `Integer` has no such limit.
- **Cleaner code.** Returning `group.getSpec().getPriority()` directly is simpler than formatting and parsing strings.

### 2. Keep null-coalescing behavior: null priority -> 0

The current index function coalesces null to `"0"`. The new function coalesces null to `0` (integer). This preserves the existing equality semantics: a group with no explicit priority is indexed as `0`.

### 3. Do not add a corresponding index to `Photo`

`Photo` also has `spec.priority` (an `Integer`) but does not declare an index for it. Adding one is out of scope for this change.

## Risks / Trade-offs

- **[Risk]** Existing index data in a running Halo instance is keyed as strings. After the plugin restarts with the new scheme, Halo will rebuild the index with integer keys.
  → **Mitigation:** Halo's scheme manager triggers index rebuild automatically on registration change. The index is small (one entry per group) and the rebuild is fast.

- **[Risk]** A downstream consumer (outside this repo) could depend on the index value being a string.
  → **Mitigation:** The index is an internal implementation detail of the plugin. No public API exposes raw index values.
