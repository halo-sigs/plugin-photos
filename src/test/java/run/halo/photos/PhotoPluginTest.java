package run.halo.photos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import run.halo.app.extension.Metadata;

class PhotoPluginTest {

    @Test
    void exifTimePresentShouldUseExif() {
        var photo = photo("a", "2026-05-02T00:00:00Z", "2020-01-01T00:00:00Z");

        assertThat(PhotoPlugin.computeEffectiveTimeIndex(photo))
            .isEqualTo("2020-01-01T00:00:00Z");
    }

    @Test
    void exifTimeMissingShouldFallBackToCreationTimestamp() {
        var photo = photo("a", "2026-05-02T00:00:00Z", null);

        assertThat(PhotoPlugin.computeEffectiveTimeIndex(photo))
            .isEqualTo("2026-05-02T00:00:00Z");
    }

    @Test
    void neitherTimePresentShouldReturnEmptyString() {
        var photo = photo("a", null, null);

        assertThat(PhotoPlugin.computeEffectiveTimeIndex(photo)).isEmpty();
    }

    @Test
    void nullExifShouldFallBackToCreationTimestamp() {
        var metadata = new Metadata();
        metadata.setName("a");
        metadata.setCreationTimestamp(Instant.parse("2026-05-02T00:00:00Z"));

        var photo = new Photo();
        photo.setMetadata(metadata);
        photo.setSpec(new Photo.PhotoSpec());

        assertThat(PhotoPlugin.computeEffectiveTimeIndex(photo))
            .isEqualTo("2026-05-02T00:00:00Z");
    }

    @Test
    void nullMetadataAndExifShouldReturnEmptyString() {
        var photo = new Photo();
        photo.setSpec(new Photo.PhotoSpec());

        assertThat(PhotoPlugin.computeEffectiveTimeIndex(photo)).isEmpty();
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
