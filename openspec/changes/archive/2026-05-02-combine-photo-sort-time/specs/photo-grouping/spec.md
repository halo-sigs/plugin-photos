## MODIFIED Requirements

### Requirement: Theme Finder Honors Optional Group
The `photoFinder` theme APIs SHALL keep returning all photos when no group is requested and SHALL provide a way to fetch ungrouped photos when callers ask for them. The default sort order SHALL use each photo's effective photo time descending: `spec.dateTimeOriginal` when present, otherwise `metadata.creationTimestamp`.

#### Scenario: List photos without group filter
- **WHEN** a theme calls `photoFinder.list(page, size)` without specifying a group
- **THEN** the result includes both grouped and ungrouped photos, ordered by effective photo time descending

#### Scenario: List photos for a specific group
- **WHEN** a theme calls `photoFinder.listBy(groupName)` with a non-empty group name
- **THEN** the result includes only photos whose `spec.groupName` equals that name, ordered by effective photo time descending

#### Scenario: List all photos
- **WHEN** a theme calls `photoFinder.listAll()`
- **THEN** all photos are returned ordered by effective photo time descending
