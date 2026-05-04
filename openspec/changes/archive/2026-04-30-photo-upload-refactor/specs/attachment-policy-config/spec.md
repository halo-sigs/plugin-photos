## ADDED Requirements

### Requirement: Settings form includes attachment configuration
The system SHALL provide form fields in plugin settings for attachment storage configuration.

#### Scenario: Admin views plugin settings
- **WHEN** admin navigates to plugin settings page
- **THEN** the form displays fields for `policyName` (storage policy) and `groupName` (attachment group)
- **AND** both fields are required for upload functionality

### Requirement: Upload service uses configured policy
The system SHALL use the configured attachment policy and group when uploading files.

#### Scenario: Upload uses configured settings
- **WHEN** the upload endpoint processes a file
- **THEN** it reads `policyName` and `groupName` from plugin settings via ReactiveSettingFetcher
- **AND** passes them to AttachmentService.upload()
