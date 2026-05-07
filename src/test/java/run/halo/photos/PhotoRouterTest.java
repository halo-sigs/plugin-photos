package run.halo.photos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.result.view.View;
import org.springframework.web.reactive.result.view.ViewResolver;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.photos.finders.PhotoFinder;
import run.halo.photos.finders.PhotoPublicQueryService;
import run.halo.photos.vo.PhotoVo;

class PhotoRouterTest {

    private final PhotoFinder photoFinder = mock(PhotoFinder.class);

    private final PhotoPublicQueryService photoPublicQueryService = mock(
        PhotoPublicQueryService.class);

    private final ReactiveSettingFetcher settingFetcher = mock(ReactiveSettingFetcher.class);

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        var router = new PhotoRouter(photoFinder, photoPublicQueryService, settingFetcher);
        var strategies = HandlerStrategies.builder()
            .viewResolver(new StubViewResolver())
            .build();
        webTestClient = WebTestClient.bindToRouterFunction(router.photoRouter())
            .handlerStrategies(strategies)
            .configureClient()
            .build();

        lenient().when(photoFinder.groupBy()).thenReturn(Flux.empty());
        ObjectNode baseSetting = new ObjectMapper().createObjectNode();
        baseSetting.put("pageSize", 10);
        baseSetting.put("title", "图库");
        lenient().when(settingFetcher.getSettingValue(eq("base"))).thenReturn(Mono.just(baseSetting));
    }

    @ParameterizedTest(name = "windowStart(idx={0}, total={1}) -> {2}")
    @CsvSource({
        // middle index
        "10, 20, 8",
        // head index
        "0, 20, 0",
        "1, 20, 0",
        // tail index
        "19, 20, 15",
        "18, 20, 15",
        // total fewer than 5
        "0, 3, 0",
        "2, 3, 0",
        // total exactly 5
        "0, 5, 0",
        "2, 5, 0",
        "4, 5, 0"
    })
    void windowStartHandlesAllPositions(int currentIndex, int total, int expectedStart) {
        assertThat(PhotoRouter.windowStart(currentIndex, total, 5)).isEqualTo(expectedStart);
    }

    @Test
    void detailReturns404WhenPhotoMissing() {
        when(photoPublicQueryService.getByName("missing"))
            .thenReturn(Mono.error(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found")));

        webTestClient.get().uri("/photos/missing")
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void detailReturns404WhenPhotoSoftDeleted() {
        when(photoPublicQueryService.getByName("gone"))
            .thenReturn(Mono.error(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found")));

        webTestClient.get().uri("/photos/gone")
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void detailRedirectsWhenGroupMismatch() {
        var photo = photoVo("abc", "2026-05-01T00:00:00Z", null);
        photo.getSpec().setGroupName("real-group");
        when(photoPublicQueryService.getByName("abc")).thenReturn(Mono.just(photo));

        webTestClient.get().uri("/photos/abc?group=other&page=2")
            .exchange()
            .expectStatus().isFound()
            .expectHeader().value(HttpHeaders.LOCATION,
                location -> assertThat(location)
                    .isEqualTo("/photos/abc?page=2"));
    }

    @Test
    void detailRendersWhenGroupMatches() {
        var photo = photoVo("abc", "2026-05-01T00:00:00Z", null);
        photo.getSpec().setGroupName("trips");
        when(photoPublicQueryService.getByName("abc")).thenReturn(Mono.just(photo));
        when(photoPublicQueryService.listAllPhotos(any(ListOptions.class), any(Sort.class)))
            .thenReturn(Flux.fromIterable(List.of(photo)));

        webTestClient.get().uri("/photos/abc?group=trips&page=1&size=10")
            .exchange()
            .expectStatus().is2xxSuccessful();
    }

    @Test
    void legacyPaginationRedirectsWith301() {
        webTestClient.get().uri("/photos/page/2")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.MOVED_PERMANENTLY)
            .expectHeader().value(HttpHeaders.LOCATION,
                location -> assertThat(location).isEqualTo("/photos?page=2"));
    }

    @Test
    void legacyPaginationRedirectPreservesGroup() {
        webTestClient.get().uri("/photos/page/2?group=trips")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.MOVED_PERMANENTLY)
            .expectHeader().value(HttpHeaders.LOCATION,
                location -> assertThat(location).isEqualTo("/photos?page=2&group=trips"));
    }

    private static PhotoVo photoVo(String name, String creationTimestamp, String dateTimeOriginal) {
        var metadata = new Metadata();
        metadata.setName(name);
        if (creationTimestamp != null) {
            metadata.setCreationTimestamp(Instant.parse(creationTimestamp));
        }

        var spec = new Photo.PhotoSpec();
        spec.setDisplayName(name);
        spec.setUrl("/" + name + ".jpg");

        var exif = new Photo.PhotoExif();
        if (dateTimeOriginal != null) {
            exif.setDateTimeOriginal(Instant.parse(dateTimeOriginal));
        }

        var photo = new Photo();
        photo.setMetadata(metadata);
        photo.setSpec(spec);
        photo.setExif(exif);
        return PhotoVo.from(photo);
    }

    @SuppressWarnings("unused")
    private static List<Photo> mixedPhotos() {
        return List.of(
            photo("old-exif", "2026-05-02T00:00:00Z", "2020-01-01T00:00:00Z"),
            photo("new-no-exif", "2026-05-04T00:00:00Z", null),
            photo("middle-exif", "2026-05-01T00:00:00Z", "2026-05-03T00:00:00Z"),
            photo("old-no-exif", "2019-01-01T00:00:00Z", null)
        );
    }

    private static Photo photo(String name, String creationTimestamp, String dateTimeOriginal) {
        var metadata = new Metadata();
        metadata.setName(name);
        if (creationTimestamp != null) {
            metadata.setCreationTimestamp(Instant.parse(creationTimestamp));
        }

        var spec = new Photo.PhotoSpec();
        spec.setDisplayName(name);
        spec.setUrl("/" + name + ".jpg");

        var exif = new Photo.PhotoExif();
        if (dateTimeOriginal != null) {
            exif.setDateTimeOriginal(Instant.parse(dateTimeOriginal));
        }

        var photo = new Photo();
        photo.setMetadata(metadata);
        photo.setSpec(spec);
        photo.setExif(exif);
        return photo;
    }

    /**
     * Stub view resolver used by the WebTestClient harness so that handlers which call
     * {@code ServerResponse#render(...)} succeed without a real Thymeleaf engine.
     */
    private static class StubViewResolver implements ViewResolver {

        @Override
        public Mono<View> resolveViewName(String viewName, Locale locale) {
            return Mono.just(new StubView());
        }
    }

    private static class StubView implements View {

        @Override
        public List<MediaType> getSupportedMediaTypes() {
            return List.of(MediaType.TEXT_HTML);
        }

        @Override
        public Mono<Void> render(java.util.Map<String, ?> model, MediaType contentType,
            org.springframework.web.server.ServerWebExchange exchange) {
            return Mono.empty();
        }
    }
}
