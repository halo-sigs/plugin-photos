## ADDED Requirements

### Requirement: Photo console uses full-width top-to-bottom layout
The system SHALL display the photo management console with a top toolbar and full-width photo grid below.

#### Scenario: User views photo console
- **WHEN** user navigates to the photo management page
- **THEN** the group selector appears as a horizontal bar at the top
- **AND** the photo grid occupies the full width below the toolbar

### Requirement: Photo cards use custom TailwindCSS styling
The system SHALL render photo grid cards using TailwindCSS utility classes instead of VCard.

#### Scenario: View photo grid
- **WHEN** photos are displayed in grid mode
- **THEN** each photo card uses custom TailwindCSS styling
- **AND** cards show the image prominently with name overlay
- **AND** hover effects are smooth and consistent
