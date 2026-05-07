package run.halo.photos;

import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import run.halo.app.extension.Metadata;
import run.halo.photos.finders.PhotoPublicQueryService;
import run.halo.photos.vo.PhotoGroupVo;

class PhotoGroupQueryEndpointTest {

    private final PhotoPublicQueryService queryService = org.mockito.Mockito.mock(
        PhotoPublicQueryService.class);

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        var endpoint = new PhotoGroupQueryEndpoint(queryService);
        webTestClient = WebTestClient.bindToRouterFunction(endpoint.endpoint())
            .configureClient()
            .build();
    }

    @Test
    void listGroupsShouldReturn200WithPhotoCountAndNoPhotos() {
        var metadata = new Metadata();
        metadata.setName("trips");

        var spec = new PhotoGroup.PhotoGroupSpec();
        spec.setDisplayName("Trips");

        var status = new PhotoGroup.PhotoGroupStatus();
        status.setPhotoCount(5);

        var groupVo = PhotoGroupVo.builder()
            .metadata(metadata)
            .spec(spec)
            .status(status)
            .photos(null)
            .build();

        when(queryService.listGroups()).thenReturn(Flux.just(groupVo));

        webTestClient.get().uri("/photogroups")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$[0].metadata.name").isEqualTo("trips")
            .jsonPath("$[0].status.photoCount").isEqualTo(5)
            .jsonPath("$[0].photos").isEmpty();
    }

    @Test
    void listGroupsShouldReturnEmptyArrayWhenNoGroups() {
        when(queryService.listGroups()).thenReturn(Flux.empty());

        webTestClient.get().uri("/photogroups")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$").isEmpty();
    }
}
