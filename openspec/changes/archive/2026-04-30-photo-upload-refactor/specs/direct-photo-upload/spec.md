## ADDED Requirements

### Requirement: Upload interface accepts multipart file
The system SHALL expose a console API endpoint that accepts a multipart file upload and creates a Photo extension.

#### Scenario: Successful upload with configured policy
- **WHEN** user uploads an image file to `POST /apis/console.api.photo.halo.run/v1alpha1/photos/upload`
- **AND** the plugin storage policy is configured
- **THEN** the system uploads the file to Halo attachment storage
- **AND** creates a Photo extension with displayName derived from filename
- **AND** returns the created Photo

#### Scenario: Upload fails when policy not configured
- **WHEN** user uploads a file without configuring the storage policy
- **THEN** the system returns a 400 error with message indicating policy configuration is required

### Requirement: Plugin settings support attachment policy configuration
The system SHALL allow administrators to configure attachment storage policy and group via plugin settings.

#### Scenario: Admin configures upload policy
- **WHEN** admin sets `policyName` and `groupName` in plugin settings
- **THEN** subsequent uploads use the configured policy and group

### Requirement: Frontend supports direct upload via UppyUpload
The system SHALL provide a drag-and-drop upload area in the photo management console.

#### Scenario: User drags images to upload area
- **WHEN** user drags image files into the upload area
- **THEN** the system initiates upload requests concurrently
- **AND** displays upload progress
- **AND** refreshes the photo list upon completion

#### Scenario: User clicks upload area to select files
- **WHEN** user clicks the upload area
- **THEN** a file picker opens allowing selection of image files

### Requirement: Editing modal displays image preview
The system SHALL display the actual image in the photo editing modal.

#### Scenario: User opens edit modal
- **WHEN** user opens a photo editing modal
- **THEN** the modal displays a preview of the photo
- **AND** the modal does NOT display a cover input field
