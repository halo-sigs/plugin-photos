package run.halo.photos;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import run.halo.app.extension.Metadata;
import run.halo.photos.service.PhotoGroupService;

class PhotoGroupEndpointTest {

    private final PhotoGroupService photoGroupService = mock(PhotoGroupService.class);

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        var endpoint = new PhotoGroupEndpoint(photoGroupService);
        webTestClient = WebTestClient.bindToRouterFunction(endpoint.endpoint())
            .configureClient()
            .build();
    }

    @Test
    void listPhotoGroupsShouldReturn200WithArray() {
        var group = group("trips");
        when(photoGroupService.listPhotoGroup())
            .thenReturn(Mono.just(List.of(group)));

        webTestClient.get().uri("/photogroups")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$[0].metadata.name").isEqualTo("trips")
            .jsonPath("$[0].spec.displayName").isEqualTo("Trips");
    }

    @Test
    void listPhotoGroupsShouldReturnEmptyArrayWhenNoGroups() {
        when(photoGroupService.listPhotoGroup())
            .thenReturn(Mono.just(List.of()));

        webTestClient.get().uri("/photogroups")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$").isEmpty();
    }

    @Test
    void deletePhotoGroupShouldReturn200WithDeletedGroup() {
        var group = group("trips");
        when(photoGroupService.deletePhotoGroup("trips"))
            .thenReturn(Mono.just(group));

        webTestClient.delete().uri("/photogroups/trips")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.metadata.name").isEqualTo("trips");

        verify(photoGroupService).deletePhotoGroup("trips");
    }

    @Test
    void deletePhotoGroupShouldPassCorrectNameToService() {
        var group = group("vacations");
        when(photoGroupService.deletePhotoGroup("vacations"))
            .thenReturn(Mono.just(group));

        webTestClient.delete().uri("/photogroups/vacations")
            .exchange()
            .expectStatus().isOk();

        verify(photoGroupService).deletePhotoGroup("vacations");
    }

    private static PhotoGroup group(String name) {
        var metadata = new Metadata();
        metadata.setName(name);
        metadata.setCreationTimestamp(Instant.parse("2026-05-01T00:00:00Z"));

        var spec = new PhotoGroup.PhotoGroupSpec();
        spec.setDisplayName(name.substring(0, 1).toUpperCase() + name.substring(1));
        spec.setPriority(0);

        var group = new PhotoGroup();
        group.setMetadata(metadata);
        group.setSpec(spec);
        return group;
    }
}
