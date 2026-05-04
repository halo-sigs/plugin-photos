## ADDED Requirements

### Requirement: Photo editing modal uses FormKit tag select
The photo editing modal SHALL use a FormKit `type="select"` input with `tags` option for entering photo tags.

#### Scenario: Opening photo edit modal
- **WHEN** the user opens the photo editing modal
- **THEN** the tag field is rendered as a FormKit select input with tags enabled

### Requirement: Tag input suggests existing tags
The tag input SHALL load and suggest all existing tags from the current photo gallery.

#### Scenario: Typing in tag field
- **WHEN** the user types in the tag input field
- **THEN** the input suggests matching tags from all existing photos

### Requirement: Tag input allows creating new tags
The tag input SHALL allow users to create tags that do not already exist.

#### Scenario: Creating a new tag
- **WHEN** the user types a tag that does not exist and submits it
- **THEN** the new tag is added to the photo's tag list
