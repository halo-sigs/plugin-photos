## ADDED Requirements

### Requirement: Batch external link import UI entry
The system SHALL provide a "批量添加外链" option in the PhotoList page's "新增" dropdown menu.

#### Scenario: User opens the import modal
- **WHEN** user clicks the "新增" button and selects "批量添加外链"
- **THEN** a modal dialog titled "批量添加外链" opens

### Requirement: Group selection in import modal
The system SHALL allow the user to select an existing PhotoGroup or leave the photo ungrouped within the import modal.

#### Scenario: User selects a group
- **WHEN** user opens the group dropdown in the import modal
- **THEN** the dropdown lists all existing PhotoGroups plus an option for "未分组"

### Requirement: URL list input
The system SHALL provide a textarea where users can paste one external image URL per line.

#### Scenario: User pastes multiple URLs
- **WHEN** user pastes text into the URL textarea
- **THEN** each non-empty line is treated as one photo URL

### Requirement: Batch photo creation from URLs
The system SHALL create one Photo resource for each valid, non-empty URL line upon form submission, assigning the selected group and deriving the photo name from the URL.

#### Scenario: Successful batch import
- **WHEN** user clicks "确定" with a valid group and at least one non-empty URL line
- **THEN** the system creates a Photo for each URL with:
  - `spec.url` set to the provided URL
  - `spec.groupName` set to the selected group name (or empty for ungrouped)
  - `spec.name` derived from the URL's filename (last path segment), falling back to "未命名"
  - `spec.priority` set to a reasonable default (e.g., 0)

### Requirement: Import feedback
The system SHALL display a success notification indicating how many photos were created after a successful import.

#### Scenario: Import completes
- **WHEN** all photos have been created successfully
- **THEN** a success toast/notification is shown with the message "成功导入 {count} 张照片"

### Requirement: Empty and invalid URL handling
The system SHALL ignore empty lines and trim whitespace around each URL before processing.

#### Scenario: Mixed valid and empty lines
- **WHEN** user submits a list containing valid URLs, blank lines, and lines with only whitespace
- **THEN** only the valid, non-empty URLs result in photo creation; blank/whitespace-only lines are silently skipped
