package run.halo.photos.finders.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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

        when(client.listBy(eq(Photo.class), any(ListOptions.class), any(PageRequestImpl.class)))
            .thenReturn(Mono.just(new ListResult<>(1, 10, 1, List.of(photo1))));

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

        when(client.listBy(eq(Photo.class), any(ListOptions.class), any(PageRequestImpl.class)))
            .thenReturn(Mono.just(new ListResult<>(1, 10, 1, List.of(photo1))));

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

        when(client.listBy(eq(Photo.class), any(ListOptions.class), any(PageRequestImpl.class)))
            .thenReturn(Mono.just(new ListResult<>(1, 10, 1, List.of(photo1))));

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

        when(client.listBy(eq(Photo.class), any(ListOptions.class), any(PageRequestImpl.class)))
            .thenReturn(Mono.just(new ListResult<>(1, 10, 1, List.of(photo1))));

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
    void listPhotosPassesThroughDatabaseOrderForEffectiveTimeSort() {
        // Database returns photos already ordered by the effectiveTime index.
        // Photo "b" has EXIF (2026-05-03), photo "a" has only creationTimestamp (2026-05-02).
        var photo1 = photo("a", "2026-05-02T00:00:00Z", null);
        var photo2 = photo("b", "2026-05-01T00:00:00Z", "2026-05-03T00:00:00Z");

        when(client.listBy(eq(Photo.class), any(ListOptions.class), any(PageRequestImpl.class)))
            .thenReturn(Mono.just(new ListResult<>(1, 10, 2, List.of(photo2, photo1))));

        var result = service.listPhotos(ListOptions.builder().build(),
            PageRequestImpl.of(1, 10, Sort.by(Sort.Order.desc("effectiveTime")))).block();

        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getItems().get(0).getMetadata().getName()).isEqualTo("b");
        assertThat(result.getItems().get(1).getMetadata().getName()).isEqualTo("a");
    }

    @Test
    void listPhotosWithEffectiveTimeSortNeverCallsListAll() {
        when(client.listBy(eq(Photo.class), any(ListOptions.class), any(PageRequestImpl.class)))
            .thenReturn(Mono.just(new ListResult<>(List.of())));

        service.listPhotos(ListOptions.builder().build(),
            PageRequestImpl.of(1, 10, Sort.by(Sort.Order.desc("effectiveTime")))).block();

        verify(client, never()).listAll(eq(Photo.class), any(ListOptions.class), any(Sort.class));
    }

    @Test
    void listPhotosDefaultSortMatchesEffectiveTimeComparator() {
        // Fixture with interleaved EXIF and non-EXIF photos.
        var photoA = photo("a", "2026-05-02T00:00:00Z", null);
        var photoB = photo("b", "2026-05-01T00:00:00Z", "2026-05-03T00:00:00Z");
        var photoC = photo("c", "2026-05-04T00:00:00Z", null);
        var photoD = photo("d", "2026-05-03T00:00:00Z", "2019-01-01T00:00:00Z");
        var all = List.of(photoA, photoB, photoC, photoD);

        // Expected order using the in-memory comparator.
        var expected = new java.util.ArrayList<>(all);
        expected.sort(run.halo.photos.PhotoSortUtils.effectiveTimeComparator(false));

        // Mock returns photos in the DB order (same as comparator).
        when(client.listBy(eq(Photo.class), any(ListOptions.class), any(PageRequestImpl.class)))
            .thenReturn(Mono.just(
                new ListResult<>(1, 4, 4, List.of(photoC, photoB, photoA, photoD))));

        var result = service.listPhotos(ListOptions.builder().build(),
            PageRequestImpl.of(1, 4, Sort.by(
                Sort.Order.desc("effectiveTime"),
                Sort.Order.desc("metadata.creationTimestamp"),
                Sort.Order.asc("metadata.name")))).block();

        assertThat(result).isNotNull();
        assertThat(result.getItems().stream()
            .map(p -> p.getMetadata().getName()).toList())
            .containsExactlyElementsOf(expected.stream()
                .map(p -> p.getMetadata().getName()).toList());
    }

    @Test
    void listPhotosShouldPaginate() {
        var photos = List.of(
            photo("c", "2026-05-02T00:00:00Z", null),
            photo("d", "2026-05-01T00:00:00Z", null)
        );

        when(client.listBy(eq(Photo.class), any(ListOptions.class), any(PageRequestImpl.class)))
            .thenReturn(Mono.just(new ListResult<>(2, 2, 4, photos)));

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
    void getByNameShouldThrowNotFoundForSoftDeletedPhoto() {
        var photo = photo("gone", "2026-05-01T00:00:00Z", null);
        photo.getMetadata().setDeletionTimestamp(Instant.parse("2026-05-01T00:00:00Z"));

        when(client.get(Photo.class, "gone")).thenReturn(Mono.just(photo));

        assertThatThrownBy(() -> service.getByName("gone").block())
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> {
                var re = (ResponseStatusException) ex;
                assertThat(re.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            });
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

        var result = service.listGroups().collectList().block();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPhotos()).isNull();
        assertThat(result.get(0).getStatus().getPhotoCount()).isEqualTo(1);
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
        group.setStatus(new PhotoGroup.PhotoGroupStatus());
        return group;
    }
}
