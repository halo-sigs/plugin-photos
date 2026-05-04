# photo-grouping Specification

## Purpose
TBD - created by archiving change make-photo-group-optional. Update Purpose after archive.
## Requirements
### Requirement: Optional Photo Group Assignment
The Photo resource SHALL allow `spec.groupName` to be empty or unset. The schema, validation rules, and persistence layer MUST accept and store photos without a group assignment.

#### Scenario: Photo created without a group
- **WHEN** a client creates a Photo with `spec.groupName` omitted, null, or empty
- **THEN** the resource is persisted successfully and is treated as ungrouped

#### Scenario: Existing grouped photo retains its group
- **WHEN** a Photo already has a non-empty `spec.groupName`
- **THEN** the resource keeps that group assignment with no migration required

### Requirement: Console Photo Editing With Optional Group
The console photo editing modal SHALL render the group selector as an optional control. The user MUST be able to save a photo with no group selected and to clear an existing group.

#### Scenario: Save photo without group
- **WHEN** the user opens the photo editing modal and submits without selecting a group
- **THEN** the form passes validation and the saved photo has no group assignment

#### Scenario: Clear an existing group
- **WHEN** the user opens an existing photo whose group is set and selects the "no group" option
- **THEN** the saved photo has no group assignment

### Requirement: Console Upload Without Group
The console upload and attachment-import flows SHALL accept photos without a group. The flow MUST NOT require the user to create or select a group before uploading.

#### Scenario: Drag-and-drop upload with no group selected
- **WHEN** the user opens the upload modal while the "all photos" / ungrouped view is active and uploads files
- **THEN** the resulting photos are created without a group assignment

#### Scenario: Attachment import with no group selected
- **WHEN** the user picks attachments while the "all photos" / ungrouped view is active
- **THEN** the imported photos are created without a group assignment

### Requirement: Console Lists All Photos Without Group Selection
The console photo list SHALL show photos when no specific group is active, including a dedicated way to view ungrouped photos. The list MUST NOT require a selected group before showing any results.

#### Scenario: Default view with no groups defined
- **WHEN** the user opens the photo console and no `PhotoGroup` exists
- **THEN** the photo list still loads and shows all photos (which are necessarily ungrouped)

#### Scenario: Switch to ungrouped view
- **WHEN** the user selects the "未分组" / ungrouped entry in the group sidebar
- **THEN** the photo list shows only photos whose `spec.groupName` is empty or unset

#### Scenario: Switch to "all photos" view
- **WHEN** the user selects the "全部" / all-photos entry in the group sidebar (if provided)
- **THEN** the photo list shows photos from every group plus ungrouped photos

### Requirement: Inline Group Edit Allows Clearing
The list-mode inline editor for `groupName` SHALL allow the user to clear the value back to an unassigned state.

#### Scenario: Clear group inline
- **WHEN** the user clicks the group cell of a photo in list mode and selects the "no group" option
- **THEN** the photo is updated with an empty `spec.groupName`

### Requirement: Group list sorted by priority descending
The `PhotoGroup` list endpoint SHALL sort results by `spec.priority` in descending order (higher values first). When priorities are equal, it MUST fall back to `metadata.creationTimestamp` descending, then `metadata.name` ascending.

#### Scenario: List groups with different priorities
- **WHEN** the console requests the group list and groups exist with priorities `10`, `5`, and `0`
- **THEN** the returned list is ordered: `10`, `5`, `0`

#### Scenario: List groups with identical priorities
- **WHEN** the console requests the group list and two groups both have priority `5`
- **THEN** the group with the later `creationTimestamp` appears first; if timestamps are also equal, alphabetical by `name`

### Requirement: Group Delete Cascade Excludes Ungrouped Photos
Deleting a `PhotoGroup` SHALL only delete photos whose `spec.groupName` matches the deleted group. Photos without a group MUST NOT be deleted by any group cascade.

#### Scenario: Delete a group with grouped and ungrouped photos in the system
- **WHEN** the user deletes a `PhotoGroup`
- **THEN** only photos whose `spec.groupName` equals that group are removed; ungrouped photos remain

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

### Requirement: Verification of Optional Group Behavior
The change SHALL be verified through console smoke checks covering the main flows that exercise optional grouping.

#### Scenario: Console verification flow
- **WHEN** verification is performed at `http://127.0.0.1:8090/console`
- **THEN** the checks cover: uploading a photo without any group, editing a photo to clear its group, viewing the ungrouped list, deleting a group while ungrouped photos remain intact, and confirming inline group edit can clear values

