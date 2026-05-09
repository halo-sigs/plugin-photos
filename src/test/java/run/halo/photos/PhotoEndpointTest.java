package run.halo.photos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.Metadata;
import run.halo.photos.service.PhotoService;
import run.halo.photos.service.PhotoUploadService;

class PhotoEndpointTest {

    private final PhotoService photoService = mock(PhotoService.class);
    private final PhotoUploadService photoUploadService = mock(PhotoUploadService.class);

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        var endpoint = new PhotoEndpoint(photoService, photoUploadService);
        webTestClient = WebTestClient.bindToRouterFunction(endpoint.endpoint())
            .configureClient()
            .build();
    }

    @Test
    void listPhotosShouldReturn200WithListResult() {
        var photo = photo("p1", "2026-05-01T00:00:00Z");
        when(photoService.listPhoto(any(PhotoQuery.class)))
            .thenReturn(Mono.just(new ListResult<>(1, 10, 1, List.of(photo))));

        webTestClient.get().uri("/photos")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.total").isEqualTo(1)
            .jsonPath("$.items[0].metadata.name").isEqualTo("p1");
    }

    @Test
    void listPhotosShouldPassGroupFilterToService() {
        var photo = photo("p1", "2026-05-01T00:00:00Z");
        when(photoService.listPhoto(any(PhotoQuery.class)))
            .thenReturn(Mono.just(new ListResult<>(1, 10, 1, List.of(photo))));

        webTestClient.get().uri("/photos?group=trips")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.total").isEqualTo(1);
    }

    @Test
    void listTagsShouldReturn200WithAllTags() {
        when(photoService.listAllTags(any(PhotoQuery.class)))
            .thenReturn(Flux.just("sunset", "beach", "mountain"));

        webTestClient.get().uri("/photos/tags")
            .exchange()
            .expectStatus().isOk()
            .expectBody(List.class)
            .consumeWith(response -> {
                var body = response.getResponseBody();
                assertThat(body).hasSize(3);
            });
    }

    @Test
    void listTagsShouldFilterByNameCaseInsensitively() {
        when(photoService.listAllTags(any(PhotoQuery.class)))
            .thenReturn(Flux.just("Sunset", "Sunrise", "beach"));

        webTestClient.get().uri("/photos/tags?name=sun")
            .exchange()
            .expectStatus().isOk()
            .expectBody(List.class)
            .consumeWith(response -> {
                var body = response.getResponseBody();
                assertThat(body).hasSize(2);
                assertThat(body).containsExactlyInAnyOrder("Sunset", "Sunrise");
            });
    }

    @Test
    void listTagsShouldReturnEmptyListWhenNoMatch() {
        when(photoService.listAllTags(any(PhotoQuery.class)))
            .thenReturn(Flux.just("sunset", "beach"));

        webTestClient.get().uri("/photos/tags?name=mountain")
            .exchange()
            .expectStatus().isOk()
            .expectBody(List.class)
            .consumeWith(response -> assertThat(response.getResponseBody()).isEmpty());
    }

    @Test
    void uploadPhotoWithoutFileShouldReturn4xx() {
        var builder = new MultipartBodyBuilder();
        builder.part("group", "trips");  // no "file" part

        webTestClient.post().uri("/photos/upload")
            .bodyValue(builder.build())
            .exchange()
            .expectStatus().is4xxClientError();
    }

    @Test
    void deletePhotoShouldReturn200() {
        var photo = photo("p1", "2026-05-01T00:00:00Z");
        when(photoService.deletePhoto("p1", false)).thenReturn(Mono.just(photo));

        webTestClient.delete().uri("/photos/p1")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.metadata.name").isEqualTo("p1");
    }

    @Test
    void deletePhotoWithAttachmentShouldPassFlagToService() {
        var photo = photo("p1", "2026-05-01T00:00:00Z");
        when(photoService.deletePhoto("p1", true)).thenReturn(Mono.just(photo));

        webTestClient.delete().uri("/photos/p1?withAttachment=true")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.metadata.name").isEqualTo("p1");
    }

    @Test
    void reextractExifShouldReturn200() {
        var photo = photo("p1", "2026-05-01T00:00:00Z");
        when(photoService.reextractExif("p1")).thenReturn(Mono.just(photo));

        webTestClient.post().uri("/photos/p1/reextract-exif")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.metadata.name").isEqualTo("p1");
    }

    private static Photo photo(String name, String creationTimestamp) {
        var metadata = new Metadata();
        metadata.setName(name);
        metadata.setCreationTimestamp(Instant.parse(creationTimestamp));

        var spec = new Photo.PhotoSpec();
        spec.setDisplayName(name);
        spec.setUrl("/" + name + ".jpg");

        var photo = new Photo();
        photo.setMetadata(metadata);
        photo.setSpec(spec);
        return photo;
    }
}
