## ADDED Requirements

### Requirement: View mode toggle
The system SHALL provide a toggle to switch between grid and list view modes.

#### Scenario: Switch to list view
- **WHEN** user clicks the list view button
- **THEN** the photo display switches to a table layout
- **AND** the view mode preference is persisted (localStorage or URL query)

#### Scenario: Switch to grid view
- **WHEN** user clicks the grid view button
- **THEN** the photo display switches to the card grid layout

### Requirement: List view columns
The system SHALL display photos in a table with columns for name, group, dateTimeOriginal, make/model, and tags.

#### Scenario: View list mode
- **WHEN** user is in list view mode
- **THEN** photos are displayed in rows with columns: checkbox, name, group, date taken, camera, tags

### Requirement: Inline editing in list view
The system SHALL support inline editing of photo name, group, and tags in list view.

#### Scenario: Edit name inline
- **WHEN** user clicks on a photo name in list view
- **THEN** the name becomes an editable text field
- **AND** on blur, the change is saved automatically

#### Scenario: Edit group inline
- **WHEN** user clicks on the group cell in list view
- **THEN** a dropdown of available groups appears
- **AND** selecting a group moves the photo to that group

#### Scenario: Edit tags inline
- **WHEN** user clicks on the tags cell in list view
- **THEN** a tag input component appears
- **AND** changes are saved on blur
