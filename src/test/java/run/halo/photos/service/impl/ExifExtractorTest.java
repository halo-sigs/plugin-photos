package run.halo.photos.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExifExtractorTest {

    private final ExifExtractor extractor = new ExifExtractor();

    @Test
    void extractExifFromEmptyBytesShouldReturnNoData() {
        var result = extractor.extractExif(new byte[0]);
        assertThat(result.hasData()).isFalse();
    }

    @Test
    void extractExifFromNonImageBytesShouldReturnNoData() {
        var result = extractor.extractExif("not an image".getBytes());
        assertThat(result.hasData()).isFalse();
    }

    @Test
    void toPhotoExifWithNoDataShouldReturnNull() {
        var exifData = extractor.extractExif(new byte[0]);
        var photoExif = extractor.toPhotoExif(exifData);
        assertThat(photoExif).isNull();
    }
}
