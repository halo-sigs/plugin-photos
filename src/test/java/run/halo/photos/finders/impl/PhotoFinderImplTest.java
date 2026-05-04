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
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.PageRequestImpl;
import run.halo.photos.Photo;
import run.halo.photos.PhotoGroup;
import run.halo.photos.finders.PhotoPublicQueryService;
import run.halo.photos.vo.PhotoGroupVo;
import run.halo.photos.vo.PhotoVo;

class PhotoFinderImplTest {

    private final PhotoPublicQueryService queryService = org.mockito.Mockito.mock(
        PhotoPublicQueryService.class);

    private PhotoFinderImpl finder;

    @BeforeEach
    void setUp() {
        finder = new PhotoFinderImpl(queryService);
    }

    @Test
    void listAllShouldReturnAllPhotos() {
        when(queryService.listPhotos(any(ListOptions.class), any(PageRequestImpl.class)))
            .thenReturn(Mono.just(new ListResult<>(1, Integer.MAX_VALUE, 4,
                mixedPhotos().stream().map(PhotoVo::from).toList())));

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
    void listShouldPaginatePhotos() {
        var photoVos = mixedPhotos().stream().map(PhotoVo::from).toList();
        when(queryService.listPhotos(any(ListOptions.class), any(PageRequestImpl.class)))
            .thenReturn(Mono.just(new ListResult<>(1, 2, 4, photoVos.subList(0, 2))));

        var result = finder.list(1, 2).block();

        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(names(result.getItems())).containsExactly("new-no-exif", "middle-exif");
    }

    @Test
    void listByShouldReturnPhotosInGroup() {
        var photoVos = mixedPhotos().stream().map(PhotoVo::from).toList();
        when(queryService.listPhotos(any(ListOptions.class), any(PageRequestImpl.class)))
            .thenReturn(Mono.just(new ListResult<>(1, Integer.MAX_VALUE, 4, photoVos)));

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
    void groupByShouldPopulatePhotosArrayForEachGroup() {
        var groupVo = PhotoGroupVo.builder()
            .metadata(group("travel").getMetadata())
            .spec(group("travel").getSpec())
            .status(new PhotoGroup.PostGroupStatus())
            .photos(List.of())
            .build();
        var photoVos = mixedPhotos().stream().map(PhotoVo::from).toList();

        when(queryService.listGroups(any(ListOptions.class), any(PageRequestImpl.class)))
            .thenReturn(Mono.just(new ListResult<>(1, Integer.MAX_VALUE, 1, List.of(groupVo))));
        when(queryService.listPhotos(any(ListOptions.class), any(PageRequestImpl.class)))
            .thenReturn(Mono.just(new ListResult<>(1, Integer.MAX_VALUE, 4, photoVos)));

        var groups = finder.groupBy().collectList().block();

        assertThat(groups).singleElement().satisfies(g -> {
            assertThat(g.getStatus().getPhotoCount()).isEqualTo(4);
            assertThat(names(g.getPhotos()))
                .containsExactly("new-no-exif", "middle-exif", "old-exif", "old-no-exif");
        });
    }

    @Test
    void groupByShouldEmitGroupsWithNonEmptyPhotosArrays() {
        var groupVo = PhotoGroupVo.builder()
            .metadata(group("travel").getMetadata())
            .spec(group("travel").getSpec())
            .status(new PhotoGroup.PostGroupStatus())
            .photos(List.of())
            .build();
        var photoVos = mixedPhotos().stream().map(PhotoVo::from).toList();

        when(queryService.listGroups(any(ListOptions.class), any(PageRequestImpl.class)))
            .thenReturn(Mono.just(new ListResult<>(1, Integer.MAX_VALUE, 1, List.of(groupVo))));
        when(queryService.listPhotos(any(ListOptions.class), any(PageRequestImpl.class)))
            .thenReturn(Mono.just(new ListResult<>(1, Integer.MAX_VALUE, 4, photoVos)));

        var groups = finder.groupBy().collectList().block();

        assertThat(groups).isNotNull();
        assertThat(groups).hasSize(1);
        assertThat(groups.get(0).getPhotos()).isNotEmpty();
    }

    @Test
    void listAllShouldReturnSameSetAndOrderAsBeforeRefactor() {
        var photoVos = mixedPhotos().stream().map(PhotoVo::from).toList();
        when(queryService.listPhotos(any(ListOptions.class), any(PageRequestImpl.class)))
            .thenReturn(Mono.just(new ListResult<>(1, Integer.MAX_VALUE, 4, photoVos)));

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

    private static List<String> names(List<PhotoVo> photos) {
        return photos.stream()
            .map(photo -> photo.getMetadata().getName())
            .toList();
    }

    private static List<Photo> mixedPhotos() {
        return List.of(
            photo("new-no-exif", "2026-05-04T00:00:00Z", null),
            photo("middle-exif", "2026-05-01T00:00:00Z", "2026-05-03T00:00:00Z"),
            photo("old-exif", "2026-05-02T00:00:00Z", "2020-01-01T00:00:00Z"),
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
