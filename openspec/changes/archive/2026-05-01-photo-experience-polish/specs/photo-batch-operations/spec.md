## ADDED Requirements

### Requirement: Photos can be moved to another group individually
The system SHALL allow users to change the group of a single photo via the edit modal.

#### Scenario: Move photo to another group
- **WHEN** user opens a photo edit modal
- **THEN** a group dropdown is visible
- **AND** selecting a different group and saving moves the photo to that group

### Requirement: Batch operations toolbar
The system SHALL display a batch operations toolbar when one or more photos are selected.

#### Scenario: Batch delete photos
- **WHEN** user selects multiple photos via checkbox
- **THEN** a toolbar appears with a delete button
- **AND** clicking delete shows a confirmation dialog
- **AND** confirmed deletion removes all selected photos

#### Scenario: Batch move photos to group
- **WHEN** user selects multiple photos
- **THEN** the toolbar includes a "Move to group" dropdown
- **AND** selecting a group moves all selected photos to that group

#### Scenario: Batch add tags
- **WHEN** user selects multiple photos
- **THEN** the toolbar includes an "Add tags" input
- **AND** submitting adds the specified tags to all selected photos

### Requirement: Search uses server-side only
The system SHALL remove client-side Fuse.js search and use server-side keyword search exclusively.

#### Scenario: Search photos
- **WHEN** user enters a keyword in the search box
- **THEN** the system sends the keyword to the server API
- **AND** displays paginated results from the server
- **AND** the total count matches the search results
