package run.halo.photos.vo;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import run.halo.app.extension.Metadata;
import run.halo.photos.Photo;

class PhotoVoTest {

    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setSerializationInclusion(
            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
    }

    @Test
    void fromShouldNotMutateSourcePhotoExif() {
        var photo = photoWithGps();

        PhotoVo.from(photo);

        assertThat(photo.getExif().getGpsLatitude()).isEqualTo(39.9);
        assertThat(photo.getExif().getGpsLongitude()).isEqualTo(116.4);
        assertThat(photo.getExif().getGpsAltitude()).isEqualTo(50.0);
    }

    @Test
    void fromShouldHideGpsFieldsInVo() {
        var photo = photoWithGps();

        var vo = PhotoVo.from(photo);

        assertThat(vo.getExif().getGpsLatitude()).isNull();
        assertThat(vo.getExif().getGpsLongitude()).isNull();
        assertThat(vo.getExif().getGpsAltitude()).isNull();
        assertThat(vo.getExif().getMake()).isEqualTo("Canon");
        assertThat(vo.getExif().getModel()).isEqualTo("EOS R5");
    }

    @Test
    void serializedJsonShouldNotContainGpsKeys() throws Exception {
        var photo = photoWithGps();
        var vo = PhotoVo.from(photo);

        String json = objectMapper.writeValueAsString(vo);
        Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});

        @SuppressWarnings("unchecked")
        Map<String, Object> exif = (Map<String, Object>) map.get("exif");
        assertThat(exif).isNotNull();
        assertThat(exif).doesNotContainKeys("gpsLatitude", "gpsLongitude", "gpsAltitude");
        assertThat(exif).containsEntry("make", "Canon");
    }

    private static Photo photoWithGps() {
        var metadata = new Metadata();
        metadata.setName("gps-photo");
        metadata.setCreationTimestamp(Instant.parse("2026-05-01T00:00:00Z"));

        var spec = new Photo.PhotoSpec();
        spec.setDisplayName("GPS Photo");
        spec.setUrl("/gps-photo.jpg");

        var exif = new Photo.PhotoExif();
        exif.setMake("Canon");
        exif.setModel("EOS R5");
        exif.setDateTimeOriginal(Instant.parse("2026-04-01T00:00:00Z"));
        exif.setGpsLatitude(39.9);
        exif.setGpsLongitude(116.4);
        exif.setGpsAltitude(50.0);

        var photo = new Photo();
        photo.setMetadata(metadata);
        photo.setSpec(spec);
        photo.setExif(exif);
        return photo;
    }
}
