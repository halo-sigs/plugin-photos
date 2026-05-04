## ADDED Requirements

### Requirement: View-only users cannot perform mutations
Users who hold only `plugin:photos:view` SHALL NOT be presented with UI elements that trigger mutations (create, update, delete) on photos or photo groups.

#### Scenario: View-only user sees read-only photo list
- **WHEN** a user with only `plugin:photos:view` opens the photo list page
- **THEN** the user sees photos and groups but no add buttons, no checkboxes, no inline editing triggers, and no group edit/delete actions

#### Scenario: View-only user sees read-only photo details
- **WHEN** a view-only user clicks a photo to open the editing modal
- **THEN** the modal opens in read-only mode with no save button

### Requirement: Manage users retain full access
Users who hold `plugin:photos:manage` SHALL see all mutation UI elements.

#### Scenario: Manage user sees editable photo list
- **WHEN** a user with `plugin:photos:manage` opens the photo list page
- **THEN** the user sees checkboxes, inline editing triggers, group edit/delete actions, and the add button dropdown

## MODIFIED Requirements

## REMOVED Requirements
