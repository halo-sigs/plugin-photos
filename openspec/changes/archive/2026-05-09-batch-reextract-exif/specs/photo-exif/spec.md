## ADDED Requirements

### Requirement: Console supports re-extracting EXIF from existing photos

The system SHALL provide a console API endpoint that re-reads EXIF data from the local attachment file of an existing Photo and updates the Photo's `exif` field.

#### Scenario: Re-extract EXIF for a photo with a local attachment
- **WHEN** the console calls `POST /apis/console.api.photo.halo.run/v1alpha1/photos/{name}/reextract-exif` for a Photo whose `spec.url` matches a local Attachment's `status.permalink`
- **THEN** the system SHALL read the attachment file from the local filesystem
- **THEN** the system SHALL parse the file's EXIF data using the same parsing logic as the upload flow
- **THEN** the system SHALL update the Photo's `exif` field with the newly parsed data
- **THEN** the system SHALL return the updated Photo

#### Scenario: Re-extract EXIF for a photo without a local attachment
- **WHEN** the console calls the re-extract endpoint for a Photo whose `spec.url` does not match any local Attachment, or the file does not exist on disk
- **THEN** the system SHALL return the Photo unchanged
- **THEN** the response status SHALL still be successful (the operation is a no-op, not an error)

#### Scenario: Batch re-extract via single-item endpoint
- **WHEN** the console selects multiple photos and invokes the re-extract endpoint for each one concurrently
- **THEN** each successful call SHALL update that photo's EXIF independently
- **THEN** failures for individual photos (missing attachment, unreadable file, etc.) SHALL NOT affect other photos in the batch
