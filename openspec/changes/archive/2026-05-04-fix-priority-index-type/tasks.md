## 1. Update index type in PhotoPlugin

- [x] 1.1 Change `IndexSpecs.<PhotoGroup, String>single("spec.priority", String.class)` to `IndexSpecs.<PhotoGroup, Integer>single("spec.priority", Integer.class)` in `PhotoPlugin.java`.
- [x] 1.2 Update the `indexFunc` to return `Integer` directly: `group.getSpec() == null || group.getSpec().getPriority() == null ? 0 : group.getSpec().getPriority()` (replacing the current `String.valueOf(0)` / `toString()` logic).

## 2. Verify

- [x] 2.1 Run `./gradlew build` and confirm compilation succeeds.
- [x] 2.2 Run `./gradlew test` and confirm all tests pass.
- [x] 2.3 Confirm `git diff` shows only the type parameter and the index function body changed in `PhotoPlugin.java`.
