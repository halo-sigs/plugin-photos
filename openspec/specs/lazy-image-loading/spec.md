## ADDED Requirements

### Requirement: Photo grid defers image loads until near viewport
The photo grid SHALL use native lazy loading so images only load when close to the viewport.

#### Scenario: Grid with many photos
- **WHEN** a photo grid renders more than 50 images
- **THEN** images outside the viewport SHALL NOT initiate network requests until the user scrolls them into view

### Requirement: Images decode off the main thread
The photo grid SHALL mark images for asynchronous decoding to prevent frame drops.

#### Scenario: Scrolling through thumbnails
- **WHEN** a user scrolls through a grid containing many thumbnail images
- **THEN** image decoding SHALL NOT cause visible frame drops or jank
