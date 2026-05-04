## ADDED Requirements

### Requirement: PhotoSpec supports tags field
The system SHALL support a `tags` field on PhotoSpec as a list of strings.

#### Scenario: Create photo with tags
- **WHEN** a Photo is created with tags `["风景", "旅行"]`
- **THEN** the tags are stored in `spec.tags`

#### Scenario: Query photos by tag
- **WHEN** user filters photos by tag via fieldSelector
- **THEN** the system returns only photos matching the specified tag
- **AND** the query uses the `spec.tags` multi-value index

### Requirement: Tags aggregation endpoint
The system SHALL provide an endpoint that returns all unique tags.

#### Scenario: Retrieve all tags
- **WHEN** user requests `GET /apis/console.api.photo.halo.run/v1alpha1/photos/tags`
- **THEN** the system returns a sorted list of all unique tag strings across all photos

### Requirement: Frontend tag input with autocomplete
The system SHALL provide a tag input component that supports adding tags and autocomplete from existing tags.

#### Scenario: Add tags to photo
- **WHEN** user types in the tag input of a photo edit modal
- **THEN** the system suggests existing tags that match the input
- **AND** user can select an existing tag or create a new one

#### Scenario: Remove tag from photo
- **WHEN** user clicks the remove button on a tag
- **THEN** the tag is removed from the photo's tag list

### Requirement: Tags displayed in grid cards
The system SHALL display up to 3 tags on photo grid cards.

#### Scenario: View photo grid
- **WHEN** photos are displayed in grid mode
- **THEN** each card displays up to 3 tags
- **AND** additional tags are indicated with "+N" badge
