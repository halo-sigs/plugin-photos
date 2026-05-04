## MODIFIED Requirements

### Requirement: Console Lists All Photos Without Group Selection
No change to this requirement.

### Requirement: Theme Finder Honors Optional Group
No change to this requirement.

## MODIFIED Requirements

### Requirement: Group list sorted by priority descending
The `PhotoGroup` list endpoint SHALL sort results by `spec.priority` in descending order (higher values first). When priorities are equal, it MUST fall back to `metadata.creationTimestamp` descending, then `metadata.name` ascending.

#### Scenario: List groups with different priorities
- **WHEN** the console requests the group list and groups exist with priorities `10`, `5`, and `0`
- **THEN** the returned list is ordered: `10`, `5`, `0`

#### Scenario: List groups with identical priorities
- **WHEN** the console requests the group list and two groups both have priority `5`
- **THEN** the group with the later `creationTimestamp` appears first; if timestamps are also equal, alphabetical by `name`
