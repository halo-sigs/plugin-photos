## ADDED Requirements

### Requirement: Console toolbar supports tag filtering
The system SHALL provide a tag filter input in the photo console toolbar that filters the displayed photos by tag.

#### Scenario: User filters photos by tag
- **WHEN** the user selects or types a tag in the tag filter
- **THEN** the photo list refreshes to show only photos matching the selected tag

#### Scenario: Clearing tag filter
- **WHEN** the user clears the tag filter
- **THEN** all photos in the current group are displayed
