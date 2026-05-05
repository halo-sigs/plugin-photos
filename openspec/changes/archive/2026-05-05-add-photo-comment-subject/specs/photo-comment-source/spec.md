## ADDED Requirements

### Requirement: Photo is recognized as a comment subject
The system SHALL register `Photo` as a valid comment subject in Halo's comment system so that comments whose `subjectRef` points to a `Photo` can be resolved and displayed correctly.

#### Scenario: Comment subject resolution
- **WHEN** Halo's comment system encounters a comment with `subjectRef.group="core.halo.run"`, `subjectRef.kind="Photo"`, and `subjectRef.name="example-photo"`
- **THEN** the system SHALL resolve the subject to the corresponding `Photo` extension

#### Scenario: Subject display metadata
- **WHEN** Halo's comment list UI requests display metadata for a comment on a photo
- **THEN** the system SHALL return a `SubjectDisplay` containing:
  - the photo's `spec.displayName` as the content title
  - the photo's detail page URL `/photos/{name}` as the permalink
  - `"照片"` as the subject type label

### Requirement: PhotoCommentSubject implements the CommentSubject contract
The system SHALL provide a `PhotoCommentSubject` class that implements `CommentSubject<Photo>` with the following methods:

#### Scenario: Fetching a photo by name
- **WHEN** `get("photo-name")` is called
- **THEN** the system SHALL return a `Mono<Photo>` fetched from `ReactiveExtensionClient`

#### Scenario: Checking Ref support
- **WHEN** `supports(Ref)` is called with a Ref whose group is `core.halo.run` and kind is `Photo`
- **THEN** the method SHALL return `true`

#### Scenario: Rejecting unsupported Refs
- **WHEN** `supports(Ref)` is called with a Ref whose group or kind does not match Photo's GVK
- **THEN** the method SHALL return `false`
