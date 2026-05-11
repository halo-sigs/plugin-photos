## ADDED Requirements

### Requirement: List Page Model Variables Are Loaded On-Demand

The `listHandler()` in `PhotoRouter` SHALL wrap reactive model variables (`groups`, `photos`, `title`) in `org.thymeleaf.context.LazyContextVariable` so that the underlying database queries execute only when the Thymeleaf template actually references those variables.

#### Scenario: Template uses all list variables

- **WHEN** a visitor requests `GET /photos` and the `photos.html` template contains expressions referencing `${groups}`, `${photos}`, and `${title}`
- **THEN** all three underlying queries execute, and the template renders normally with the resolved data

#### Scenario: Template does not use list variables

- **WHEN** a visitor requests `GET /photos` and the `photos.html` template does not reference `${groups}`, `${photos}`, or `${title}` (for example, an SPA theme that fetches data client-side via the public API)
- **THEN** none of the underlying database queries for groups, photos, or title execute

### Requirement: Detail Page Model Variables Are Loaded On-Demand

The `renderDetail()` method in `PhotoRouter` SHALL wrap reactive model variables (`neighbors`, `prev`, `next`, `position`, `total`, `title`) in `org.thymeleaf.context.LazyContextVariable` so that the underlying database query for the filtered photo list executes only when the Thymeleaf template references those variables.

#### Scenario: Template uses all detail variables

- **WHEN** a visitor requests `GET /photos/{name}` and the `photo.html` template contains expressions referencing `${neighbors}`, `${prev}`, `${next}`, `${position}`, `${total}`, or `${title}`
- **THEN** the underlying filtered photo list query executes, and the template renders normally with the resolved data

#### Scenario: Template does not use detail variables

- **WHEN** a visitor requests `GET /photos/{name}` and the `photo.html` template does not reference `${neighbors}`, `${prev}`, `${next}`, `${position}`, `${total}`, or `${title}` (for example, an SPA theme that fetches data client-side via the public API)
- **THEN** the underlying filtered photo list query does not execute

### Requirement: Synchronous Model Entries Remain Unchanged

Variables that do not trigger reactive database or service calls (`group`, `page`, `size`, `photo`, `backUrl`, `photoUrl`, `_templateId`) SHALL remain as direct values in the model map and SHALL NOT be wrapped in `LazyContextVariable`.

#### Scenario: Template accesses synchronous variables

- **WHEN** a template references `${group}`, `${page}`, `${size}`, `${photo}`, `${backUrl}`, `${photoUrl}`, or `${_templateId}`
- **THEN** those values are available immediately without any deferred resolution

### Requirement: Lazy Loading Uses A Bounded Timeout

Each `LazyContextVariable.loadValue()` implementation SHALL block the reactive pipeline with a bounded timeout (defaulting to `ReactiveUtils.DEFAULT_TIMEOUT` or a locally defined `BLOCKING_TIMEOUT` constant). The timeout SHALL prevent an unbounded thread hang if the underlying reactive source stalls.

#### Scenario: Underlying query completes within timeout

- **WHEN** the template accesses a lazy variable and the underlying query completes within the configured timeout
- **THEN** the variable resolves successfully and the template renders

#### Scenario: Underlying query exceeds timeout

- **WHEN** the template accesses a lazy variable and the underlying query does not complete within the configured timeout
- **THEN** a timeout exception is raised and the request fails fast instead of blocking the thread indefinitely
