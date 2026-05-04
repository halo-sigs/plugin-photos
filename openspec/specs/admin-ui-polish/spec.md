## Requirements

### Requirement: Cohesive Toolbar Layout
The admin photo console SHALL provide a cohesive toolbar where search, tag filtering, view switching, batch status, pagination context, and primary actions remain aligned, consistently spaced, and usable across supported viewport widths.

#### Scenario: Toolbar with all controls visible
- **WHEN** the user opens the photo management console with at least one group and photos
- **THEN** the toolbar presents filtering, view switching, and creation actions in a visually consistent layout without overlapping controls

#### Scenario: Toolbar on narrow viewport
- **WHEN** the console is viewed on a narrow viewport
- **THEN** toolbar controls wrap or stack into usable rows without clipped labels, overlapping controls, or inaccessible primary actions

### Requirement: Polished Group Navigation
The admin photo console SHALL render photo groups with clear active, hover, drag, empty, loading, and action states that preserve the user's ability to scan and manage groups.

#### Scenario: Active group is visible
- **WHEN** a group is selected
- **THEN** the selected group is visually distinct from other groups and its photo count remains readable

#### Scenario: Group actions remain stable
- **WHEN** the user hovers, drags, edits, or deletes a group
- **THEN** group actions and status indicators remain in predictable positions without shifting neighboring content unexpectedly

### Requirement: Refined Photo Grid
The admin photo console SHALL render grid photo cards with stable dimensions, readable names and tags, clear selection state, and explicit loading or deletion feedback.

#### Scenario: Grid photo selected
- **WHEN** the user selects one or more photos in grid mode
- **THEN** each selected photo shows a clear selection indicator without making the card resize or obscuring the image preview unnecessarily

#### Scenario: Grid card metadata shown
- **WHEN** a photo has a display name and tags
- **THEN** the card presents that metadata in a readable way that does not overlap primary selection or deletion indicators

### Requirement: Refined Photo List
The admin photo console SHALL render list mode as a dense, scannable management table with aligned columns, consistent row states, and inline editing controls that fit within the table layout.

#### Scenario: List row selected
- **WHEN** the user selects a photo in list mode
- **THEN** the row selection state is visible and does not reduce readability of thumbnail, name, group, EXIF, or tag fields

#### Scenario: Inline edit active
- **WHEN** the user edits name, group, or tags inline
- **THEN** the edit control fits the row layout and provides clear save, cancel, focus, and error behavior

### Requirement: Stable Batch Operation Surface
The admin photo console SHALL expose batch operations in a compact command surface that appears when photos are selected and includes progress, disabled, and completion states without causing avoidable layout jumps.

#### Scenario: Batch toolbar appears
- **WHEN** the user selects photos
- **THEN** batch actions become available in a stable region that communicates the selected count and available operations

#### Scenario: Batch operation running
- **WHEN** a batch operation is in progress
- **THEN** the relevant controls are disabled or show progress and the user can see that work is ongoing

### Requirement: Polished Photo Modals
The admin photo console SHALL present photo editing, upload, and group editing modals with consistent visual hierarchy, form spacing, preview areas, section dividers, validation messages, and action placement.

#### Scenario: Photo edit modal opened
- **WHEN** the user opens an existing photo for editing
- **THEN** the preview, general fields, EXIF information, metadata, and actions are arranged with clear hierarchy and no clipped content

#### Scenario: Upload modal opened
- **WHEN** the user opens the upload flow
- **THEN** the upload target, upload area, progress feedback, and completion actions are visually clear and aligned with the rest of the console

### Requirement: Admin UI Verification
The admin photo console UI polish SHALL be verified through local interaction checks covering the main management flows and responsive behavior.

#### Scenario: Main flow smoke check
- **WHEN** the implementation is complete
- **THEN** the verification covers group selection, filtering, view switching, batch selection, modal open/close, upload entry, and empty/loading states

#### Scenario: Responsive smoke check
- **WHEN** the implementation is complete
- **THEN** the verification includes both desktop and narrow viewport checks for overlap, clipping, and unusable controls
