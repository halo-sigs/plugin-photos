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
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.photos.Photo;
import run.halo.photos.PhotoGroup;

class PhotoPublicQueryServiceImplTest {

    private final ReactiveExtensionClient client = org.mockito.Mockito.mock(
        ReactiveExtensionClient.class);

    private PhotoPublicQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PhotoPublicQueryServiceImpl(client);
    }

    @Test
    void listPhotosShouldFilterByGroup() {
        var photo1 = photo("a", "2026-05-01T00:00:00Z", null);
        photo1.getSpec().setGroupName("trips");

        when(client.listAll(eq(Photo.class), any(ListOptions.class), eq(Sort.unsorted())))
            .thenReturn(Flux.just(photo1));

        var options = ListOptions.builder()
            .andQuery(run.halo.app.extension.index.query.Queries.equal("spec.groupName", "trips"))
            .build();
        var result = service.listPhotos(options, PageRequestImpl.of(1, 10, Sort.unsorted()))
            .block();

        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getSpec().getGroupName()).isEqualTo("trips");
    }

    @Test
    void listPhotosShouldFilterUngrouped() {
        var photo1 = photo("a", "2026-05-01T00:00:00Z", null);
        photo1.getSpec().setGroupName("");

        when(client.listAll(eq(Photo.class), any(ListOptions.class), eq(Sort.unsorted())))
            .thenReturn(Flux.just(photo1));

        var options = ListOptions.builder()
            .andQuery(run.halo.app.extension.index.query.Queries.equal("spec.groupName", ""))
            .build();
        var result = service.listPhotos(options, PageRequestImpl.of(1, 10, Sort.unsorted()))
            .block();

        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getMetadata().getName()).isEqualTo("a");
    }

    @Test
    void listPhotosShouldFilterByTag() {
        var photo1 = photo("a", "2026-05-01T00:00:00Z", null);
        photo1.getSpec().setTags(List.of("sunset"));

        when(client.listAll(eq(Photo.class), any(ListOptions.class), eq(Sort.unsorted())))
            .thenReturn(Flux.just(photo1));

        var options = ListOptions.builder()
            .andQuery(run.halo.app.extension.index.query.Queries.equal("spec.tags", "sunset"))
            .build();
        var result = service.listPhotos(options, PageRequestImpl.of(1, 10, Sort.unsorted()))
            .block();

        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getMetadata().getName()).isEqualTo("a");
    }

    @Test
    void listPhotosShouldFilterByKeyword() {
        var photo1 = photo("a", "2026-05-01T00:00:00Z", null);
        photo1.getSpec().setDisplayName("Beach Sunset");

        when(client.listAll(eq(Photo.class), any(ListOptions.class), eq(Sort.unsorted())))
            .thenReturn(Flux.just(photo1));

        var options = ListOptions.builder()
            .andQuery(run.halo.app.extension.index.query.Queries.contains("spec.displayName", "beach"))
            .build();
        var result = service.listPhotos(options, PageRequestImpl.of(1, 10, Sort.unsorted()))
            .block();

        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getSpec().getDisplayName()).isEqualTo("Beach Sunset");
    }

    @Test
    void listPhotosShouldUseDefaultSortWithNullExifTime() {
        var photo1 = photo("a", "2026-05-02T00:00:00Z", null);
        var photo2 = photo("b", "2026-05-01T00:00:00Z", "2026-05-03T00:00:00Z");

        when(client.listAll(eq(Photo.class), any(ListOptions.class), eq(Sort.unsorted())))
            .thenReturn(Flux.just(photo1, photo2));

        var result = service.listPhotos(ListOptions.builder().build(),
            PageRequestImpl.of(1, 10, Sort.unsorted())).block();

        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(2);
        // photo with EXIF time should come first (effective time 2026-05-03)
        assertThat(result.getItems().get(0).getMetadata().getName()).isEqualTo("b");
        // photo without EXIF falls back to creation time (2026-05-02)
        assertThat(result.getItems().get(1).getMetadata().getName()).isEqualTo("a");
    }

    @Test
    void listPhotosShouldPaginate() {
        var photos = List.of(
            photo("a", "2026-05-04T00:00:00Z", null),
            photo("b", "2026-05-03T00:00:00Z", null),
            photo("c", "2026-05-02T00:00:00Z", null),
            photo("d", "2026-05-01T00:00:00Z", null)
        );

        when(client.listAll(eq(Photo.class), any(ListOptions.class), eq(Sort.unsorted())))
            .thenReturn(Flux.fromIterable(photos));

        var result = service.listPhotos(ListOptions.builder().build(),
            PageRequestImpl.of(2, 2, Sort.unsorted())).block();

        assertThat(result).isNotNull();
        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getItems().get(0).getMetadata().getName()).isEqualTo("c");
        assertThat(result.getItems().get(1).getMetadata().getName()).isEqualTo("d");
    }

    @Test
    void getByNameShouldReturnEmptyForSoftDeletedPhoto() {
        var photo = photo("gone", "2026-05-01T00:00:00Z", null);
        photo.getMetadata().setDeletionTimestamp(Instant.parse("2026-05-01T00:00:00Z"));

        when(client.fetch(Photo.class, "gone")).thenReturn(Mono.just(photo));

        var result = service.getByName("gone").blockOptional();
        assertThat(result).isEmpty();
    }

    @Test
    void listGroupsShouldNotPopulatePhotos() {
        var group = group("trips");
        var photo = photo("a", "2026-05-01T00:00:00Z", null);
        photo.getSpec().setGroupName("trips");

        when(client.listAll(eq(PhotoGroup.class), any(ListOptions.class), eq(Sort.unsorted())))
            .thenReturn(Flux.just(group));
        when(client.listBy(eq(Photo.class), any(ListOptions.class), any(PageRequestImpl.class)))
            .thenReturn(Mono.just(new ListResult<>(1, 1, 1, List.of(photo))));

        var result = service.listGroups(ListOptions.builder().build(),
            PageRequestImpl.of(1, 10, Sort.unsorted())).block();

        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getPhotos()).isNull();
        assertThat(result.getItems().get(0).getStatus().getPhotoCount()).isEqualTo(1);
    }

    @Test
    void listTagsShouldReturnCountsAndApplyFilter() {
        var photo1 = photo("a", "2026-05-01T00:00:00Z", null);
        photo1.getSpec().setTags(List.of("sunset", "beach"));
        var photo2 = photo("b", "2026-05-01T00:00:00Z", null);
        photo2.getSpec().setTags(List.of("sunset", "mountain"));
        var photo3 = photo("c", "2026-05-01T00:00:00Z", null);
        photo3.getSpec().setTags(List.of("mountain"));

        when(client.listAll(eq(Photo.class), any(ListOptions.class), eq(Sort.unsorted())))
            .thenReturn(Flux.just(photo1, photo2, photo3));

        var tags = service.listTags(null).collectList().block();
        assertThat(tags).isNotNull();
        assertThat(tags).hasSize(3);
        assertThat(tags.stream().filter(t -> t.getName().equals("sunset")).findFirst())
            .hasValueSatisfying(t -> assertThat(t.getPhotoCount()).isEqualTo(2));
        assertThat(tags.stream().filter(t -> t.getName().equals("beach")).findFirst())
            .hasValueSatisfying(t -> assertThat(t.getPhotoCount()).isEqualTo(1));
        assertThat(tags.stream().filter(t -> t.getName().equals("mountain")).findFirst())
            .hasValueSatisfying(t -> assertThat(t.getPhotoCount()).isEqualTo(2));
    }

    @Test
    void listTagsShouldFilterCaseInsensitively() {
        var photo1 = photo("a", "2026-05-01T00:00:00Z", null);
        photo1.getSpec().setTags(List.of("Sunset", "Sunrise"));

        when(client.listAll(eq(Photo.class), any(ListOptions.class), eq(Sort.unsorted())))
            .thenReturn(Flux.just(photo1));

        var tags = service.listTags("sun").collectList().block();
        assertThat(tags).isNotNull();
        assertThat(tags).hasSize(2);
        assertThat(tags.stream().map(run.halo.photos.vo.PhotoTagVo::getName))
            .containsExactlyInAnyOrder("Sunset", "Sunrise");
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

    private static PhotoGroup group(String name) {
        var metadata = new Metadata();
        metadata.setName(name);

        var spec = new PhotoGroup.PhotoGroupSpec();
        spec.setDisplayName(name);
        spec.setPriority(0);

        var group = new PhotoGroup();
        group.setMetadata(metadata);
        group.setSpec(spec);
        group.setStatus(new PhotoGroup.PostGroupStatus());
        return group;
    }
}
