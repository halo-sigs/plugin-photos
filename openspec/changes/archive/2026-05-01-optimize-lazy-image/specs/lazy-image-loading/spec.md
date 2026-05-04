## ADDED Requirements

### Requirement: Component defers image load until near viewport
The LazyImage component SHALL defer fetching image bytes until the element is close to the viewport.

#### Scenario: Image below the fold
- **WHEN** a LazyImage is rendered outside the visible viewport
- **THEN** the browser SHALL NOT initiate a network request for the image until the user scrolls it into view

### Requirement: Component shows loading state while fetching
The LazyImage component SHALL display a loading placeholder while the image is being fetched.

#### Scenario: Image enters viewport
- **WHEN** a LazyImage scrolls into view and the browser begins fetching the image
- **THEN** the component SHALL render the loading slot content until the image finishes loading

#### Scenario: Image finishes loading
- **WHEN** the image download completes successfully
- **THEN** the component SHALL replace the loading placeholder with the rendered image

### Requirement: Component shows error state on failure
The LazyImage component SHALL display an error placeholder if the image fails to load.

#### Scenario: Image request fails
- **WHEN** the image URL returns an error or the download fails
- **THEN** the component SHALL render the error slot content and keep the loading state hidden

### Requirement: Component uses async decoding
The LazyImage component SHALL mark images for asynchronous decoding to avoid blocking the main thread.

#### Scenario: Grid with many thumbnails
- **WHEN** a grid contains more than 50 LazyImage instances
- **THEN** image decoding SHALL NOT cause visible frame drops during scroll or interaction
