## ADDED Requirements

### Requirement: Console Upload Preserves Selected Group
The console direct-upload endpoint (`POST /apis/console.api.photo.halo.run/v1alpha1/photos/upload`) SHALL honor the group value submitted by the upload UI so that the resulting `Photo.spec.groupName` equals the user's selection. The endpoint MUST accept the `group` value carried as a `multipart/form-data` field of the same name; it MAY also accept a `group` query parameter as a fallback when the multipart field is absent. When neither is provided or both are empty, the photo MUST be created as ungrouped (`spec.groupName` empty/unset).

#### Scenario: Upload with a group selected creates a grouped photo
- **WHEN** the user opens the upload modal, picks a non-empty group from the "上传到分组" selector, and uploads one or more files
- **THEN** every resulting `Photo` has `spec.groupName` equal to the selected group's `metadata.name`

#### Scenario: Upload with the "未分组" option keeps photos ungrouped
- **WHEN** the user uploads files with the "未分组" entry selected (empty `group` value)
- **THEN** the resulting photos are created with an empty/unset `spec.groupName`

#### Scenario: Direct API call carrying group as a multipart form field
- **WHEN** an API client posts `multipart/form-data` to the upload endpoint with both a `file` part and a `group` form field set to an existing group name
- **THEN** the server reads the value from the multipart field and persists it to `Photo.spec.groupName`

#### Scenario: Direct API call carrying group as a query parameter
- **WHEN** an API client posts to `/photos/upload?group=<name>` with `multipart/form-data` containing only the `file` part (no `group` form field)
- **THEN** the server falls back to the query parameter and persists `<name>` to `Photo.spec.groupName`

#### Scenario: Multipart field overrides query parameter
- **WHEN** the request supplies BOTH a `group` multipart field and a `group` query parameter with different values
- **THEN** the server uses the multipart field's value and ignores the query value

### Requirement: Upload Endpoint OpenAPI Documents `group` As A Multipart Field
The OpenAPI declaration of the upload route SHALL describe the request body as `multipart/form-data` with at minimum a required `file` field and an optional `group` string field, so the regenerated TypeScript SDK and any third-party consumers see `group` in the body rather than in the query string.

#### Scenario: Regenerated SDK reflects the body field
- **WHEN** `./gradlew generateApiClient` is run after the endpoint change
- **THEN** the resulting `uploadPhoto` client method exposes `group` as a body/form field (not a URL query parameter), matching the server's canonical input

#### Scenario: OpenAPI viewer shows the multipart shape
- **WHEN** a developer inspects the upload endpoint in the Halo OpenAPI viewer
- **THEN** the request body is documented as `multipart/form-data` containing `file` (binary, required) and `group` (string, optional)
