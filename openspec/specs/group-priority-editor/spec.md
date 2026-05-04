# group-priority-editor Specification

## Purpose
TBD - created by archiving change add-group-priority-field. Update Purpose after archive.
## Requirements
### Requirement: Group creation form exposes priority input
The group creation form SHALL render a numeric input field for `priority`. The field MUST accept integer values. The default value SHOULD be `0`. The submitted value MUST be persisted as `spec.priority` on the created `PhotoGroup`.

#### Scenario: Create group with custom priority
- **WHEN** the user opens the group creation modal, enters a display name, sets priority to `10`, and submits
- **THEN** the new group is created with `spec.priority` equal to `10`

#### Scenario: Create group without changing priority
- **WHEN** the user opens the group creation modal, enters a display name, leaves priority at the default `0`, and submits
- **THEN** the new group is created with `spec.priority` equal to `0`

### Requirement: Group editing form exposes priority input
The group editing form SHALL render the current `spec.priority` value in a numeric input field. The user MUST be able to change the value. On save, the updated priority MUST be persisted.

#### Scenario: Edit group priority
- **WHEN** the user opens the group editing modal for an existing group, changes priority from `5` to `20`, and submits
- **THEN** the group is updated with `spec.priority` equal to `20`

#### Scenario: Edit group without changing priority
- **WHEN** the user opens the group editing modal for an existing group, changes only the display name, and submits
- **THEN** the group retains its existing `spec.priority` value
