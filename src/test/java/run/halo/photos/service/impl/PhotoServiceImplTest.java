package run.halo.photos.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Sort;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.PageRequest;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.photos.Photo;
import run.halo.photos.PhotoQuery;

class PhotoServiceImplTest {

    private final ReactiveExtensionClient client = org.mockito.Mockito.mock(
        ReactiveExtensionClient.class);

    private PhotoServiceImpl photoService;

    @BeforeEach
    void setUp() {
        photoService = new PhotoServiceImpl(client);
    }

    @Test
    void defaultSortShouldUseEffectiveTimeBeforePagination() {
        when(client.listAll(eq(Photo.class), any(ListOptions.class), any(Sort.class)))
            .thenReturn(Flux.fromIterable(mixedPhotos()));

        var result = photoService.listPhoto(query("/photos?page=1&size=2")).block();

        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(names(result.getItems())).containsExactly("new-no-exif", "middle-exif");
        verify(client).listAll(eq(Photo.class), any(ListOptions.class), any(Sort.class));
        verify(client, never()).listBy(eq(Photo.class), any(ListOptions.class),
            any(PageRequest.class));
    }

    @Test
    void explicitShootingTimeAscendingShouldUseEffectiveTime() {
        when(client.listAll(eq(Photo.class), any(ListOptions.class), any(Sort.class)))
            .thenReturn(Flux.fromIterable(mixedPhotos()));

        var result = photoService.listPhoto(
            query("/photos?page=1&size=4&sort=spec.dateTimeOriginal,asc")).block();

        assertThat(result).isNotNull();
        assertThat(names(result.getItems())).containsExactly(
            "old-no-exif",
            "old-exif",
            "middle-exif",
            "new-no-exif"
        );
        verify(client).listAll(eq(Photo.class), any(ListOptions.class), any(Sort.class));
        verify(client, never()).listBy(eq(Photo.class), any(ListOptions.class),
            any(PageRequest.class));
    }

    @Test
    void creationTimeSortShouldKeepPagedFieldSortPath() {
        when(client.listBy(eq(Photo.class), any(ListOptions.class), any(PageRequest.class)))
            .thenReturn(Mono.just(new ListResult<>(List.of())));

        photoService.listPhoto(query("/photos?sort=metadata.creationTimestamp,asc")).block();

        var pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
        verify(client).listBy(eq(Photo.class), any(ListOptions.class),
            pageRequestCaptor.capture());
        verify(client, never()).listAll(eq(Photo.class), any(ListOptions.class),
            any(Sort.class));
        assertThat(pageRequestCaptor.getValue().getSort()
            .getOrderFor("metadata.creationTimestamp"))
            .extracting(Sort.Order::isAscending)
            .isEqualTo(true);
    }

    private static List<String> names(List<Photo> photos) {
        return photos.stream()
            .map(photo -> photo.getMetadata().getName())
            .toList();
    }

    private static List<Photo> mixedPhotos() {
        return List.of(
            photo("old-exif", "2026-05-02T00:00:00Z", "2020-01-01T00:00:00Z"),
            photo("new-no-exif", "2026-05-04T00:00:00Z", null),
            photo("middle-exif", "2026-05-01T00:00:00Z", "2026-05-03T00:00:00Z"),
            photo("old-no-exif", "2019-01-01T00:00:00Z", null)
        );
    }

    private static PhotoQuery query(String uri) {
        var request = MockServerHttpRequest.get(uri).build();
        return new PhotoQuery(MockServerWebExchange.from(request));
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
