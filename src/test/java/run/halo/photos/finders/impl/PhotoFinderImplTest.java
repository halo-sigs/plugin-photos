package run.halo.photos.finders.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.photos.Photo;
import run.halo.photos.PhotoGroup;
import run.halo.photos.vo.PhotoVo;

class PhotoFinderImplTest {

    private final ReactiveExtensionClient client = org.mockito.Mockito.mock(
        ReactiveExtensionClient.class);

    private PhotoFinderImpl finder;

    @BeforeEach
    void setUp() {
        finder = new PhotoFinderImpl(client);
    }

    @Test
    void listAllShouldSortByEffectiveTimeDescending() {
        when(client.listAll(eq(Photo.class), any(ListOptions.class), any(Sort.class)))
            .thenReturn(Flux.fromIterable(mixedPhotos()));

        var names = finder.listAll()
            .map(PhotoVo::getMetadata)
            .map(metadata -> metadata.getName())
            .collectList()
            .block();

        assertThat(names).containsExactly(
            "new-no-exif",
            "middle-exif",
            "old-exif",
            "old-no-exif"
        );
    }

    @Test
    void listShouldSortBeforePagination() {
        when(client.listAll(eq(Photo.class), any(ListOptions.class), any(Sort.class)))
            .thenReturn(Flux.fromIterable(mixedPhotos()));

        var result = finder.list(1, 2).block();

        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(names(result.getItems())).containsExactly("new-no-exif", "middle-exif");
    }

    @Test
    void listByShouldSortGroupedPhotosByEffectiveTimeDescending() {
        when(client.listAll(eq(Photo.class), any(ListOptions.class), any(Sort.class)))
            .thenReturn(Flux.fromIterable(mixedPhotos()));

        var names = finder.listBy("travel")
            .map(PhotoVo::getMetadata)
            .map(metadata -> metadata.getName())
            .collectList()
            .block();

        assertThat(names).containsExactly(
            "new-no-exif",
            "middle-exif",
            "old-exif",
            "old-no-exif"
        );
    }

    @Test
    void groupByShouldSortPhotosInsideEachGroupByEffectiveTimeDescending() {
        when(client.listAll(eq(PhotoGroup.class), any(ListOptions.class), any(Sort.class)))
            .thenReturn(Flux.just(group("travel")));
        when(client.listAll(eq(Photo.class), any(ListOptions.class), any(Sort.class)))
            .thenReturn(Flux.fromIterable(mixedPhotos()));

        var groups = finder.groupBy().collectList().block();

        assertThat(groups).singleElement().satisfies(group -> {
            assertThat(group.getStatus().getPhotoCount()).isEqualTo(4);
            assertThat(names(group.getPhotos()))
                .containsExactly("new-no-exif", "middle-exif", "old-exif", "old-no-exif");
        });
    }

    private static List<String> names(List<PhotoVo> photos) {
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

    private static PhotoGroup group(String name) {
        var metadata = new Metadata();
        metadata.setName(name);

        var spec = new PhotoGroup.PhotoGroupSpec();
        spec.setDisplayName(name);

        var group = new PhotoGroup();
        group.setMetadata(metadata);
        group.setSpec(spec);
        group.setStatus(new PhotoGroup.PostGroupStatus());
        return group;
    }

    private static Photo photo(String name, String creationTimestamp, String dateTimeOriginal) {
        var metadata = new Metadata();
        metadata.setName(name);
        metadata.setCreationTimestamp(parse(creationTimestamp));

        var spec = new Photo.PhotoSpec();
        spec.setDisplayName(name);
        spec.setUrl("/" + name + ".jpg");
        spec.setGroupName("travel");

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
