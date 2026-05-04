## Why

`PhotoGroupVo.from(PhotoGroup)` is dead code with a misleading signature. The method name suggests a static factory (mirroring `PhotoVo.from(Photo)`, which returns a built `PhotoVo`), but it actually returns an unbuilt `PhotoGroupVoBuilder` and forces the caller to `.build()`. No production or test code calls it — every real construction site (`PhotoFinderImpl`, `PhotoPublicQueryServiceImpl`, and three test classes) goes through `PhotoGroupVo.builder()` directly. Keeping it costs nothing today but invites future misuse: a reader following the `PhotoVo.from` pattern would get a compile error or, worse, a partially-constructed VO if the contract were ever silently changed.

## What Changes

- Remove the `public static PhotoGroupVoBuilder from(PhotoGroup photoGroup)` method from `PhotoGroupVo` (`src/main/java/run/halo/photos/vo/PhotoGroupVo.java:24-30`).
- No replacement. Existing call sites already use `PhotoGroupVo.builder()` and continue to work unchanged.

Not breaking for theme authors: `PhotoGroupVo` is a value object consumed via finders/templates; the static helper is not part of the theme-facing surface and has no callers anywhere in the repo.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. This is an internal-only cleanup with no spec-level behavior change. No requirement in `openspec/specs/` references `PhotoGroupVo.from`.

## Impact

- **Code**: one method deletion in `PhotoGroupVo.java`. No callers to migrate.
- **Public API / theme surface**: unchanged. `PhotoGroupVo` itself, its fields, and `PhotoGroupVo.builder()` are untouched.
- **Tests**: no changes required; existing tests use `.builder()` directly.
- **Generated TS client**: unaffected (the method is not part of any HTTP DTO).
