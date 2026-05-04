package run.halo.photos;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import run.halo.app.extension.Metadata;
import run.halo.photos.vo.PhotoVo;

class PhotoUrlBuilderTest {

    @Test
    void detailUrlIncludesFullContextFromRequest() {
        var builder = builder(Map.of(
            "group", "trips",
            "page", "2",
            "size", "20"
        ));

        assertThat(builder.detail(photoVo("abc")))
            .isEqualTo("/photos/abc?group=trips&page=2&size=20");
    }

    @Test
    void detailUrlOmitsBlankGroup() {
        var builder = builder(Map.of(
            "page", "2"
        ));

        assertThat(builder.detail(photoVo("abc"))).isEqualTo("/photos/abc?page=2");
    }

    @Test
    void detailUrlOverridesReplaceRequestValues() {
        var builder = builder(Map.of(
            "group", "trips",
            "page", "2"
        ));

        assertThat(builder.detail(photoVo("abc"), Map.of("group", "other")))
            .isEqualTo("/photos/abc?group=other&page=2");
    }

    @Test
    void detailUrlOverrideWithBlankRemovesParam() {
        var builder = builder(Map.of(
            "group", "trips",
            "page", "2"
        ));

        assertThat(builder.detail(photoVo("abc"), Map.of("group", "")))
            .isEqualTo("/photos/abc?page=2");
    }

    @Test
    void detailUrlIgnoresNonWhitelistedParameters() {
        var contextParams = new LinkedHashMap<String, String>();
        contextParams.put("group", "trips");
        contextParams.put("debug", "true");

        var builder = builder(contextParams);

        assertThat(builder.detail(photoVo("abc"))).isEqualTo("/photos/abc?group=trips");
    }

    @Test
    void listUrlNoArgsReturnsBarePath() {
        var builder = builder(Map.of(
            "group", "trips",
            "page", "5"
        ));

        assertThat(builder.list()).isEqualTo("/photos");
    }

    @Test
    void listUrlWithGroupOnly() {
        var builder = builder(Map.of());

        assertThat(builder.list("trips")).isEqualTo("/photos?group=trips");
    }

    @Test
    void listUrlWithBlankGroupOmitsGroupParam() {
        var builder = builder(Map.of());

        assertThat(builder.list("")).isEqualTo("/photos");
        assertThat(builder.list((String) null)).isEqualTo("/photos");
    }

    @Test
    void listUrlWithFullPagination() {
        var builder = builder(Map.of());

        assertThat(builder.list("trips", 2, 20))
            .isEqualTo("/photos?group=trips&page=2&size=20");
    }

    @Test
    void listUrlOmitsNonPositivePagination() {
        var builder = builder(Map.of());

        assertThat(builder.list("trips", 0, 0)).isEqualTo("/photos?group=trips");
        assertThat(builder.list(null, 1, 10)).isEqualTo("/photos?page=1&size=10");
    }

    private static PhotoUrlBuilder builder(Map<String, String> contextParams) {
        return new PhotoUrlBuilder(contextParams);
    }

    private static PhotoVo photoVo(String name) {
        var metadata = new Metadata();
        metadata.setName(name);
        return PhotoVo.builder()
            .metadata(metadata)
            .permalink("/photos/" + name)
            .build();
    }
}
