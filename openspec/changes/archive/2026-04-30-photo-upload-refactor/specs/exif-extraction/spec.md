## ADDED Requirements

### Requirement: EXIF core fields stored in PhotoSpec
The system SHALL extract core EXIF fields during upload and store them in PhotoSpec.

#### Scenario: Upload with EXIF data
- **WHEN** user uploads a photo containing EXIF metadata
- **THEN** the system extracts make, model, lensModel, dateTimeOriginal, gpsLatitude, gpsLongitude, gpsAltitude, imageWidth, imageHeight, iso
- **AND** stores them in the corresponding PhotoSpec fields

#### Scenario: Upload without EXIF data
- **WHEN** user uploads a photo without EXIF metadata
- **THEN** the PhotoSpec EXIF fields remain null
- **AND** the upload completes successfully

### Requirement: Complete EXIF stored in annotations
The system SHALL store the complete EXIF metadata as JSON in photo annotations.

#### Scenario: Full EXIF preservation
- **WHEN** a photo is uploaded or created
- **THEN** the system stores all extractable EXIF data as a JSON string in `metadata.annotations["photos.halo.run/exif"]`

### Requirement: EXIF fields editable in console
The system SHALL allow users to view and edit core EXIF fields in the photo editing modal.

#### Scenario: User edits EXIF information
- **WHEN** user opens the edit modal for a photo
- **THEN** the modal displays the core EXIF fields
- **AND** allows the user to modify them
- **AND** saves changes to PhotoSpec upon submission
