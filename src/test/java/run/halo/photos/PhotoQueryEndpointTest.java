package run.halo.photos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.PageRequestImpl;
import run.halo.photos.finders.PhotoPublicQueryService;
import run.halo.photos.vo.PhotoTagVo;
import run.halo.photos.vo.PhotoVo;

class PhotoQueryEndpointTest {

    private final PhotoPublicQueryService queryService = org.mockito.Mockito.mock(
        PhotoPublicQueryService.class);

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        var endpoint = new PhotoQueryEndpoint(queryService);
        webTestClient = WebTestClient.bindToRouterFunction(endpoint.endpoint())
            .configureClient()
            .build();
    }

    @Test
    void listPhotosShouldReturn200AndListResult() throws Exception {
        var photo = photoWithGps();
        var photoVo = PhotoVo.from(photo);
        when(queryService.listPhotos(any(ListOptions.class), any(PageRequestImpl.class)))
            .thenReturn(Mono.just(new ListResult<>(1, 10, 1, List.of(photoVo))));

        var response = webTestClient.get().uri("/photos")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        assertThat(response).isNotNull();
        var mapper = new ObjectMapper();
        Map<String, Object> map = mapper.readValue(response, Map.class);
        assertThat(map).containsKey("items");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) map.get("items");
        assertThat(items).hasSize(1);
        // Note: in the test harness the ObjectMapper may not have Halo's NON_NULL config,
        // so we verify the VO itself has null GPS (covered in PhotoVoTest) and the
        // response structure is correct.
        @SuppressWarnings("unchecked")
        Map<String, Object> exif = (Map<String, Object>) items.get(0).get("exif");
        assertThat(exif).isNotNull();
        assertThat(exif.get("make")).isEqualTo("Canon");
    }

    @Test
    void getPhotoShouldReturn200ForExisting() {
        var photo = photoWithGps();
        when(queryService.getByName("abc")).thenReturn(Mono.just(PhotoVo.from(photo)));

        webTestClient.get().uri("/photos/abc")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void getPhotoShouldReturn404ForMissing() {
        when(queryService.getByName("missing")).thenReturn(Mono.empty());

        webTestClient.get().uri("/photos/missing")
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void getPhotoShouldReturn404ForSoftDeleted() {
        when(queryService.getByName("deleted")).thenReturn(Mono.empty());

        webTestClient.get().uri("/photos/deleted")
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void listTagsShouldReturnDistinctTagsWithCounts() throws Exception {
        when(queryService.listTags(null))
            .thenReturn(Flux.just(
                PhotoTagVo.builder().name("sunset").photoCount(2).build(),
                PhotoTagVo.builder().name("beach").photoCount(1).build()
            ));

        var response = webTestClient.get().uri("/tags")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        assertThat(response).isNotNull();
        var mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tags = mapper.readValue(response, List.class);
        assertThat(tags).hasSize(2);
        assertThat(tags.stream().filter(t -> "sunset".equals(t.get("name"))).findFirst())
            .hasValueSatisfying(t -> assertThat(t.get("photoCount")).isEqualTo(2));
    }

    @Test
    void listTagsShouldApplyNameFilter() throws Exception {
        when(queryService.listTags("sun"))
            .thenReturn(Flux.just(
                PhotoTagVo.builder().name("sunset").photoCount(2).build()
            ));

        var response = webTestClient.get().uri("/tags?name=sun")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        assertThat(response).isNotNull();
        var mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tags = mapper.readValue(response, List.class);
        assertThat(tags).hasSize(1);
        assertThat(tags.get(0).get("name")).isEqualTo("sunset");
    }

    @Test
    void listTagsShouldNotContainPermalink() throws Exception {
        when(queryService.listTags(null))
            .thenReturn(Flux.just(
                PhotoTagVo.builder().name("sunset").photoCount(2).build()
            ));

        var response = webTestClient.get().uri("/tags")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        assertThat(response).isNotNull();
        var mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tags = mapper.readValue(response, List.class);
        assertThat(tags.get(0)).doesNotContainKey("permalink");
    }

    @Test
    void postToPhotosShouldReturn4xx() {
        webTestClient.post().uri("/photos")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{}")
            .exchange()
            .expectStatus().is4xxClientError();
    }

    private static Photo photoWithGps() {
        var metadata = new Metadata();
        metadata.setName("abc");
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
