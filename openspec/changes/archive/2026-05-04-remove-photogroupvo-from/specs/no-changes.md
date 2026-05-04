## No Spec Changes

This change is a pure internal cleanup — deletion of a single, unused static helper method (`PhotoGroupVo.from(PhotoGroup)`) on a value object. No specification-level requirement is added, modified, or removed:

- No spec under `openspec/specs/` references `PhotoGroupVo.from`.
- The VO itself, its fields, its `Lombok @Builder`, and its `ExtensionVoOperator` contract are unchanged. Existing requirements for `PhotoFinder`, `public-api`, and theme rendering already describe behavior in terms of `PhotoGroupVo` instances; how those instances are constructed internally is an implementation concern, not a spec concern.
- Production callers (`PhotoFinderImpl`, `PhotoPublicQueryServiceImpl`) already build `PhotoGroupVo` via `PhotoGroupVo.builder()` and remain untouched.
