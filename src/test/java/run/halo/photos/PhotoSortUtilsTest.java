package run.halo.photos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import run.halo.app.extension.Metadata;

class PhotoSortUtilsTest {

    @org.junit.jupiter.api.Test
    void shouldSortByEffectiveTimeDescending() {
        var photos = new ArrayList<>(List.of(
            photo("old-exif", "2026-05-02T00:00:00Z", "2020-01-01T00:00:00Z"),
            photo("new-no-exif", "2026-05-04T00:00:00Z", null),
            photo("middle-exif", "2026-05-01T00:00:00Z", "2026-05-03T00:00:00Z"),
            photo("old-no-exif", "2019-01-01T00:00:00Z", null)
        ));

        photos.sort(PhotoSortUtils.effectiveTimeComparator(false));

        assertThat(names(photos)).containsExactly(
            "new-no-exif",
            "middle-exif",
            "old-exif",
            "old-no-exif"
        );
    }

    @org.junit.jupiter.api.Test
    void shouldSortByEffectiveTimeAscending() {
        var photos = new ArrayList<>(List.of(
            photo("old-exif", "2026-05-02T00:00:00Z", "2020-01-01T00:00:00Z"),
            photo("new-no-exif", "2026-05-04T00:00:00Z", null),
            photo("middle-exif", "2026-05-01T00:00:00Z", "2026-05-03T00:00:00Z"),
            photo("old-no-exif", "2019-01-01T00:00:00Z", null)
        ));

        photos.sort(PhotoSortUtils.effectiveTimeComparator(true));

        assertThat(names(photos)).containsExactly(
            "old-no-exif",
            "old-exif",
            "middle-exif",
            "new-no-exif"
        );
    }

    @org.junit.jupiter.api.Test
    void shouldKeepNullEffectiveTimesLast() {
        var photos = new ArrayList<>(List.of(
            photo("missing-created", null, null),
            photo("with-exif", null, "2026-05-03T00:00:00Z"),
            photo("with-created", "2026-05-02T00:00:00Z", null)
        ));

        photos.sort(PhotoSortUtils.effectiveTimeComparator(false));

        assertThat(names(photos)).containsExactly(
            "with-exif",
            "with-created",
            "missing-created"
        );
    }

    private static List<String> names(List<Photo> photos) {
        return photos.stream()
            .map(photo -> photo.getMetadata().getName())
            .toList();
    }

    private static Photo photo(String name, String creationTimestamp, String dateTimeOriginal) {
        var metadata = new Metadata();
        metadata.setName(name);
        metadata.setCreationTimestamp(parse(creationTimestamp));

        var spec = new Photo.PhotoSpec();
        spec.setDisplayName(name);
        spec.setUrl("/" + name + ".jpg");

        var exif = new Photo.PhotoExif();
        exif.setDateTimeOriginal(parse(dateTimeOriginal));

        var photo = new Photo();
        photo.setMetadata(metadata);
        photo.setSpec(spec);
        photo.setExif(exif);
        return photo;
    }

    private static Instant parse(String value) {
        return value == null ? null : Instant.parse(value);
    }
}
