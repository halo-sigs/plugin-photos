## Context

The current `PhotoPlugin.start()` registers `Photo` and `PhotoGroup` schemes using the deprecated `IndexAttributeFactory.simpleAttribute()` and manual `IndexSpec` construction. Additionally, `PhotoServiceImpl` and `PhotoFinderImpl` use `QueryFactory` for building field selector queries, which is also deprecated in favor of `Queries`.

This is a mechanical update to align with Halo's newer extension index APIs. No behavioral changes to plugin functionality.

## Goals / Non-Goals

**Goals:**
- Replace deprecated index registration APIs with `IndexSpecs.single(name, keyType)`
- Replace deprecated `QueryFactory` usage with `Queries`
- Ensure compatibility with Halo 2.22+

**Non-Goals:**
- Change any plugin behavior or API responses
- Modify frontend code
- Add new features

## Decisions

**1. Use `IndexSpecs.single()` with `String.class` key type for all existing indexes**
- Rationale: All current indexes (`spec.groupName`, `spec.displayName`, `spec.priority`) are string-based in the existing implementation. Even `priority` was converted to string via `toString()`. Keeping `String` preserves exact behavior.
- Alternative considered: Using `Integer.class` for `spec.priority`. Rejected because it changes the sort semantics (string "10" < "2" vs integer 10 > 2). Keeping string avoids risk.

**2. Use `Queries` static methods directly instead of `QueryFactory`**
- Rationale: `Queries.equal()` and `Queries.contains()` are direct replacements for `QueryFactory.equal()` and `QueryFactory.contains()`. Minimal code churn.

**3. Keep Halo platform dependency at current version or bump if compilation fails**
- Rationale: `build.gradle` already targets `2.17.0-SNAPSHOT` platform and Halo `2.22.0`. The new APIs should be available. If compile fails, bump to latest stable platform version.

## Risks / Trade-offs

- [Risk] `IndexSpecs` API may require a newer platform version than currently declared → Mitigation: Bump platform version in `build.gradle` and recompile.
- [Risk] String-typed priority index may have subtle ordering differences if the new API treats types differently → Mitigation: Verify group/photo ordering in console after change.
