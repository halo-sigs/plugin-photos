package run.halo.photos.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import run.halo.photos.PhotoGroup;
import run.halo.photos.PhotoQuery;

class PhotoGroupServiceImplTest {

    private final ReactiveExtensionClient client = mock(ReactiveExtensionClient.class);

    private PhotoGroupServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PhotoGroupServiceImpl(client);
    }

    @Test
    void listPhotoGroupShouldReturnFirstPage() {
        var groups = List.of(group("a", 2), group("b", 1), group("c", 0));
        when(client.listAll(eq(PhotoGroup.class), any(ListOptions.class), eq(Sort.unsorted())))
            .thenReturn(Flux.fromIterable(groups));
        when(client.listBy(eq(Photo.class), any(ListOptions.class), any(PageRequest.class)))
            .thenReturn(Mono.just(new ListResult<>(1, 1, 0L, List.of())));

        var result = service.listPhotoGroup(query("/groups?page=1&size=2")).block();

        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(3);
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getItems()).hasSize(2);
    }

    @Test
    void listPhotoGroupShouldReturnSecondPage() {
        var groups = List.of(group("a", 2), group("b", 1), group("c", 0));
        when(client.listAll(eq(PhotoGroup.class), any(ListOptions.class), eq(Sort.unsorted())))
            .thenReturn(Flux.fromIterable(groups));
        when(client.listBy(eq(Photo.class), any(ListOptions.class), any(PageRequest.class)))
            .thenReturn(Mono.just(new ListResult<>(1, 1, 0L, List.of())));

        var result = service.listPhotoGroup(query("/groups?page=2&size=2")).block();

        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(3);
        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getItems()).hasSize(1);
    }

    @Test
    void listPhotoGroupOutOfBoundsPageShouldFallBackToFirstPage() {
        var groups = List.of(group("a", 0), group("b", 0));
        when(client.listAll(eq(PhotoGroup.class), any(ListOptions.class), eq(Sort.unsorted())))
            .thenReturn(Flux.fromIterable(groups));
        when(client.listBy(eq(Photo.class), any(ListOptions.class), any(PageRequest.class)))
            .thenReturn(Mono.just(new ListResult<>(1, 1, 0L, List.of())));

        // page=5 is out of bounds (only 2 groups with size=2)
        var result = service.listPhotoGroup(query("/groups?page=5&size=2")).block();

        assertThat(result).isNotNull();
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getItems()).hasSize(2);
    }

    @Test
    void listPhotoGroupPopulatesPhotoCountForEachGroup() {
        var group = group("mygroup", 0);
        when(client.listAll(eq(PhotoGroup.class), any(ListOptions.class), eq(Sort.unsorted())))
            .thenReturn(Flux.just(group));
        when(client.listBy(eq(Photo.class), any(ListOptions.class), any(PageRequest.class)))
            .thenReturn(Mono.just(new ListResult<>(1, 1, 5L, List.of())));

        var result = service.listPhotoGroup(query("/groups?page=1&size=10")).block();

        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getStatusOrDefault().getPhotoCount()).isEqualTo(5);
    }

    @Test
    void deletePhotoGroupShouldCascadeDeletePhotos() {
        var group = group("mygroup", 0);
        var photo1 = photo("p1", "mygroup");
        var photo2 = photo("p2", "mygroup");
        when(client.fetch(PhotoGroup.class, "mygroup")).thenReturn(Mono.just(group));
        when(client.listAll(eq(Photo.class), any(ListOptions.class), eq(Sort.unsorted())))
            .thenReturn(Flux.just(photo1, photo2));
        when(client.delete(any(Photo.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(client.delete(group)).thenReturn(Mono.just(group));

        var result = service.deletePhotoGroup("mygroup").block();

        assertThat(result).isEqualTo(group);
        verify(client, times(2)).delete(any(Photo.class));
        verify(client).delete(group);
    }

    @Test
    void deletePhotoGroupWithNoPhotosShouldDeleteOnlyTheGroup() {
        var group = group("empty", 0);
        when(client.fetch(PhotoGroup.class, "empty")).thenReturn(Mono.just(group));
        when(client.listAll(eq(Photo.class), any(ListOptions.class), eq(Sort.unsorted())))
            .thenReturn(Flux.empty());
        when(client.delete(group)).thenReturn(Mono.just(group));

        var result = service.deletePhotoGroup("empty").block();

        assertThat(result).isEqualTo(group);
        verify(client).delete(group);
        verify(client, never()).delete(any(Photo.class));
    }

    @Test
    void deletePhotoGroupShouldReturnEmptyWhenGroupNotFound() {
        when(client.fetch(PhotoGroup.class, "missing")).thenReturn(Mono.empty());

        var result = service.deletePhotoGroup("missing").blockOptional();

        assertThat(result).isEmpty();
    }

    @Test
    void fetchPhotoCountReturnsCountFromDatabase() {
        var group = group("trips", 0);
        when(client.listBy(eq(Photo.class), any(ListOptions.class), any(PageRequest.class)))
            .thenReturn(Mono.just(new ListResult<>(1, 1, 7L, List.of())));

        var count = service.fetchPhotoCount(group).block();

        assertThat(count).isEqualTo(7);
    }

    private static PhotoQuery query(String uri) {
        var request = MockServerHttpRequest.get(uri).build();
        return new PhotoQuery(MockServerWebExchange.from(request));
    }

    private static PhotoGroup group(String name, int priority) {
        var metadata = new Metadata();
        metadata.setName(name);
        metadata.setCreationTimestamp(Instant.parse("2026-05-01T00:00:00Z"));

        var spec = new PhotoGroup.PhotoGroupSpec();
        spec.setDisplayName(name);
        spec.setPriority(priority);

        var group = new PhotoGroup();
        group.setMetadata(metadata);
        group.setSpec(spec);
        return group;
    }

    private static Photo photo(String name, String groupName) {
        var metadata = new Metadata();
        metadata.setName(name);

        var spec = new Photo.PhotoSpec();
        spec.setDisplayName(name);
        spec.setGroupName(groupName);
        spec.setUrl("/" + name + ".jpg");

        var photo = new Photo();
        photo.setMetadata(metadata);
        photo.setSpec(spec);
        return photo;
    }
}
