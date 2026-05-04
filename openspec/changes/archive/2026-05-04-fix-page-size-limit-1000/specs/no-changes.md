## No Spec Changes

This change is a pure implementation bug fix. No specification-level requirements are added, modified, or removed. The existing `public-api` spec already requires that `photoFinder.listAll()` returns the full gallery and that `photoFinder.groupBy()` embeds all photos per group; this change corrects the implementation to satisfy those existing requirements without altering the requirements themselves.
