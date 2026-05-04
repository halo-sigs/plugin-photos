## MODIFIED Requirements

### Requirement: PhotoVo exposes the exif field for theme consumption

`PhotoVo` SHALL include an `exif` field populated from `Photo.exif`, so that theme templates and the public API can access EXIF data via `photoFinder` or the `api.photo.halo.run/v1alpha1` endpoints.

The factory `PhotoVo.from(Photo)` SHALL produce a defensive copy of `Photo.exif` rather than aliasing the same instance, and SHALL force the GPS coordinate fields (`gpsLatitude`, `gpsLongitude`, `gpsAltitude`) to `null` on the copy. All other EXIF fields SHALL match the source `Photo.exif` values exactly.

The underlying `Photo.PhotoExif` type and the values stored on the `Photo` extension SHALL remain unchanged. Consumers that serialize `Photo` directly (e.g. console endpoints under `console.api.photo.halo.run`) SHALL still see GPS coordinates; only `PhotoVo` consumers see the GPS-hidden view.

JSON serialization of `PhotoVo` SHALL omit the GPS fields entirely (relying on the system-wide `Include.NON_NULL` configuration); the JSON SHALL NOT contain `gpsLatitude`, `gpsLongitude`, or `gpsAltitude` keys when those values have been nulled by `PhotoVo.from`.

#### Scenario: Theme retrieves photos with EXIF

- **WHEN** a theme calls `photoFinder.listAll()` or similar methods to obtain a photo list
- **THEN** each returned `PhotoVo` SHALL have its non-GPS `exif` fields set to the corresponding `Photo.exif` values

#### Scenario: GPS fields are hidden in PhotoVo

- **WHEN** `PhotoVo.from(photo)` is called for a `Photo` whose `exif.gpsLatitude`, `exif.gpsLongitude`, and `exif.gpsAltitude` are non-null
- **THEN** the returned `PhotoVo.exif.gpsLatitude`, `gpsLongitude`, and `gpsAltitude` SHALL all be `null`
- **THEN** the source `photo.exif.gpsLatitude`, `gpsLongitude`, and `gpsAltitude` SHALL still be their original non-null values (the source object is not mutated)

#### Scenario: GPS fields are absent from serialized PhotoVo JSON

- **WHEN** a `PhotoVo` produced by `PhotoVo.from` for a GPS-bearing `Photo` is serialized to JSON by the public API or by the `photoFinder` template path
- **THEN** the JSON object's `exif` field SHALL NOT contain the keys `gpsLatitude`, `gpsLongitude`, or `gpsAltitude`

#### Scenario: Console serialization still includes GPS

- **WHEN** the console endpoint `GET /apis/console.api.photo.halo.run/v1alpha1/photos` returns photos and a photo's `exif` contains GPS coordinates
- **THEN** the response JSON `exif` field SHALL contain the GPS coordinates (because the response payload is `Photo`, not `PhotoVo`)
