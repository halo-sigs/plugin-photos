## ADDED Requirements

### Requirement: Upload modal uses UppyUpload component
The system SHALL provide a dedicated upload modal using Halo's `UppyUpload` component for batch photo uploads.

#### Scenario: User clicks direct upload
- **WHEN** the user selects "直接上传" from the add dropdown
- **THEN** an upload modal opens with the `UppyUpload` component

### Requirement: Upload modal supports group selection
The upload modal SHALL allow the user to select which photo group the uploaded photos belong to.

#### Scenario: User uploads photos to a specific group
- **WHEN** the user opens the upload modal
- **THEN** a group selector is visible defaulting to the currently selected group
- **AND** photos uploaded through the modal are assigned to the selected group
