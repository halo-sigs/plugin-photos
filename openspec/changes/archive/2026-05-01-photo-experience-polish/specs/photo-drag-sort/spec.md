## ADDED Requirements

### Requirement: Photos support drag-and-drop sorting
The system SHALL allow users to reorder photos within a group via drag-and-drop.

#### Scenario: Drag photo to new position
- **WHEN** user drags a photo card to a new position in the grid
- **THEN** the photo moves to the dropped position
- **AND** the system updates priority values for affected photos
- **AND** the new order persists after refresh

#### Scenario: Drag in list view
- **WHEN** user is in list view mode
- **THEN** dragging a row reorders the photo in the list
- **AND** the priority is updated accordingly

### Requirement: Sorting updates priority in batch
The system SHALL update photo priorities via a batch request after drag-and-drop reordering.

#### Scenario: Save reordered priorities
- **WHEN** user completes a drag-and-drop operation
- **THEN** the system sends a batch request updating the priority of all reordered photos
