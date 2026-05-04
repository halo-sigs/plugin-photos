## MODIFIED Requirements

### Requirement: Theme Finder Honors Optional Group
The `photoFinder` theme APIs SHALL keep returning all photos when no group is requested and SHALL provide a way to fetch ungrouped photos when callers ask for them. The default sort order SHALL prioritize `spec.exifData.dateTimeOriginal` descending; when that field is absent the photo SHALL fall back to sorting by `metadata.creationTimestamp` descending.

#### Scenario: List photos without group filter
- **WHEN** a theme calls `photoFinder.list(page, size)` without specifying a group
- **THEN** the result includes both grouped and ungrouped photos, ordered by shooting time descending (falling back to creation time for photos without EXIF data)

#### Scenario: List photos for a specific group
- **WHEN** a theme calls `photoFinder.listBy(groupName)` with a non-empty group name
- **THEN** the result includes only photos whose `spec.groupName` equals that name, ordered by shooting time descending (falling back to creation time for photos without EXIF data)

#### Scenario: List all photos
- **WHEN** a theme calls `photoFinder.listAll()`
- **THEN** all photos are returned ordered by shooting time descending (falling back to creation time for photos without EXIF data)
