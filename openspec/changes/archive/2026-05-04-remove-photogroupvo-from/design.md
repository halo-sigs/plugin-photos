## Context

`PhotoGroupVo.from(PhotoGroup)` was added as a convenience static factory but never adopted. A repository-wide search confirms it has zero callers:

- Only declaration: `src/main/java/run/halo/photos/vo/PhotoGroupVo.java:24`
- Production sites that build `PhotoGroupVo` (`PhotoFinderImpl.java:75`, `PhotoPublicQueryServiceImpl.java:79`) call `PhotoGroupVo.builder()` directly.
- Test sites (`PhotoFinderImplTest`, `PhotoGroupQueryEndpointTest`) also call `PhotoGroupVo.builder()` directly.

The method's signature is also asymmetric with the analogous `PhotoVo.from(Photo)`, which returns a fully built `PhotoVo`. `PhotoGroupVo.from` returns a `PhotoGroupVoBuilder`, requiring the caller to chain `.build()`. A future reader who copies the `PhotoVo.from` pattern would be surprised.

This design is deliberately tiny because the change itself is tiny: one method deletion in one VO file.

## Goals / Non-Goals

**Goals:**
- Remove the unused `PhotoGroupVo.from(PhotoGroup)` method.
- Leave every other API on `PhotoGroupVo` (fields, generated `builder()`, `ExtensionVoOperator` contract) unchanged.

**Non-Goals:**
- Renaming `PhotoVo.from` or otherwise changing the symmetric, in-use sibling API.
- Introducing a replacement helper. Existing callers already construct `PhotoGroupVo` via `.builder()`; there is nothing to migrate.
- Touching `PhotoFinderImpl`, `PhotoPublicQueryServiceImpl`, or any test — they don't reference the deleted method.

## Decisions

### 1. Delete rather than rename

The user's initial suggestion was to rename to `toBuilder()` or `builderFrom()`. We delete instead because:

- **No callers exist.** Renaming a zero-call API only adds surface area for future misuse.
- **The repo already has a clean construction idiom** (`PhotoGroupVo.builder()` directly), used everywhere. A "fix" by rename would create a second way to do the same thing.
- **Consistent with project hygiene.** The codebase has actively removed dead code in recent commits (e.g., `36e2f75 refactor: remove dead code and orphan test asset`).

If a future use case actually wants a copy-from-extension shortcut, it can be reintroduced with a clear contract and call sites at the same time.

### 2. No replacement, no symmetry pass on `PhotoVo.from`

`PhotoVo.from(Photo)` is widely used and returns a built `PhotoVo` — it is correct as-is. We are not unifying the two VOs' construction patterns in this change.

## Risks / Trade-offs

- **[Risk]** Deletion is technically a binary-incompatible change for any external consumer that imports `run.halo.photos.vo.PhotoGroupVo` and calls `from`.
  → **Mitigation**: `PhotoGroupVo` is internal to the plugin's theme/finder layer and is not part of any documented public Java API. A grep against the plugin source finds zero callers. Theme authors interact with `PhotoGroupVo` only as a serialized template model, never via the static factory.

- **[Risk]** Future divergence: someone re-reads `PhotoVo.from` and adds a similar method back to `PhotoGroupVo` with the same broken contract.
  → **Mitigation**: Out of scope for this change. The right defense is a code-review note, not preserving the broken method.
