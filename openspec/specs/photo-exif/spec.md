## ADDED Requirements

### Requirement: Photo has a dedicated top-level exif field

`Photo` Extension SHALL have an optional `exif` field of type `PhotoExif` at the top level, as a sibling to `spec`. This field SHALL be auto-populated by the system at upload time and MAY be manually edited by the user. When an image contains no EXIF data, or when a photo is created manually via URL, the `exif` field SHALL be null.

#### Scenario: Upload image with EXIF data
- **WHEN** a user uploads an image that contains EXIF metadata
- **THEN** the system SHALL parse the image EXIF and write recognized fields into the `exif` field of the newly created `Photo`
- **THEN** `photo.spec` SHALL NOT contain any EXIF fields

#### Scenario: Upload image without EXIF data
- **WHEN** a user uploads an image that contains no EXIF metadata (e.g., a PNG screenshot)
- **THEN** the system SHALL create the `Photo` with `exif` set to null

#### Scenario: Manually create Photo via URL
- **WHEN** a user creates a Photo by entering a URL manually in the console (no upload flow)
- **THEN** the system SHALL create the `Photo` with `exif` set to null (or populated if the user fills it in manually)

---

### Requirement: PhotoExif contains a complete set of displayable EXIF fields

`PhotoExif` SHALL contain the following fields, all optional (nullable):

**Camera info**
- `make` (String): Camera brand, e.g. "Sony"
- `model` (String): Camera model, e.g. "ILCE-7M4"
- `lensModel` (String): Lens model, e.g. "FE 50mm F1.8"
- `software` (String): Post-processing software, e.g. "Adobe Lightroom"

**Shooting parameters**
- `dateTimeOriginal` (Instant): Original capture time
- `fNumber` (Double): Aperture value, e.g. 2.8
- `exposureTime` (String): Shutter speed in fractional format, e.g. "1/125"
- `iso` (Integer): ISO sensitivity, e.g. 400
- `focalLength` (Double): Focal length in mm, e.g. 50.0
- `focalLengthIn35mm` (Integer): 35mm equivalent focal length, e.g. 75
- `flash` (Integer): Flash status as EXIF standard enum code
- `whiteBalance` (Integer): White balance mode as EXIF standard enum code
- `exposureMode` (Integer): Exposure mode as EXIF standard enum code
- `exposureProgram` (Integer): Exposure program as EXIF standard enum code
- `meteringMode` (Integer): Metering mode as EXIF standard enum code

**Image dimensions**
- `imageWidth` (Integer): Width in pixels
- `imageHeight` (Integer): Height in pixels

**GPS location**
- `gpsLatitude` (Double): Latitude in decimal degrees
- `gpsLongitude` (Double): Longitude in decimal degrees
- `gpsAltitude` (Double): Altitude in meters

#### Scenario: Complete camera parameters
- **WHEN** an image's EXIF contains make, model, fNumber, exposureTime, iso, and focalLength
- **THEN** the corresponding fields in `photo.exif` SHALL be populated with values matching the EXIF source data

#### Scenario: Shutter speed format preserved
- **WHEN** an image EXIF encodes shutter speed as a rational number (e.g., 1/125)
- **THEN** `photo.exif.exposureTime` SHALL be stored as the string `"1/125"`, not as the float `0.008`

#### Scenario: GPS data extraction
- **WHEN** an image EXIF contains GPS coordinates
- **THEN** `photo.exif.gpsLatitude` and `photo.exif.gpsLongitude` SHALL be populated as decimal-degree Double values

---

### Requirement: Raw EXIF data is not written to annotations at upload time

The system SHALL NOT serialize the full raw EXIF payload and write it to `metadata.annotations`.

#### Scenario: Inspect annotations after upload
- **WHEN** a user uploads any image
- **THEN** the newly created `Photo` object's `metadata.annotations` SHALL NOT contain the key `photos.halo.run/exif`

---

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
