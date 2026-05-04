## MODIFIED Requirements

### Requirement: GET /tags Lists Distinct Tag Names With Counts
The endpoint `GET /apis/api.photo.halo.run/v1alpha1/tags` SHALL return `List<PhotoTagVo>` where each entry has shape `{ name: String, photoCount: Integer }`. Tags SHALL be the set of distinct values across all `Photo.spec.tags` arrays, excluding soft-deleted photos. `photoCount` SHALL be the number of non-deleted photos whose `spec.tags` contains that tag name.

The endpoint SHALL accept an optional `name` query parameter. When `name` is non-blank, the response SHALL include only entries whose tag name contains the parameter value (case-insensitive); when `name` is absent or blank, all distinct tags SHALL be returned.

`PhotoTagVo` SHALL NOT include a `permalink` field.

The implementation MAY serve responses from a short-lived in-memory cache. Cached responses MAY reflect a state up to 2 minutes stale. The response content and shape SHALL be identical whether served from cache or computed live.

#### Scenario: List all tags
- **WHEN** an anonymous client issues `GET /apis/api.photo.halo.run/v1alpha1/tags`
- **THEN** the response contains one entry per distinct tag name across all non-deleted photos
- **THEN** every entry has a non-null `photoCount` matching the number of non-deleted photos that carry that tag

#### Scenario: Filter tags by name substring
- **WHEN** an anonymous client issues `GET /apis/api.photo.halo.run/v1alpha1/tags?name=sun` and the gallery has tags `sunset`, `sunrise`, `mountain`
- **THEN** the response contains entries for `sunset` and `sunrise` only

#### Scenario: Tag VO has no permalink
- **WHEN** any consumer deserializes a `PhotoTagVo` returned by this endpoint
- **THEN** the object has no `permalink` field

#### Scenario: Cached response is content-equivalent
- **WHEN** the tag list is served from cache within the 2-minute TTL window
- **THEN** the response body is identical in structure and semantics to a freshly computed response
