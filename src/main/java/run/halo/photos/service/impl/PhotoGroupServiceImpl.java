package run.halo.photos.service.impl;

import static run.halo.app.extension.index.query.Queries.equal;
import static run.halo.app.extension.router.selector.SelectorUtil.labelAndFieldSelectorToListOptions;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.router.IListRequest.QueryListRequest;
import run.halo.photos.Photo;
import run.halo.photos.PhotoGroup;
import run.halo.photos.service.PhotoGroupService;

/**
 * Service implementation for {@link Photo}.
 *
 * @author LIlGG
 * @since 1.0.0
 */
@Component
class PhotoGroupServiceImpl implements PhotoGroupService {

    private final ReactiveExtensionClient client;

    public PhotoGroupServiceImpl(ReactiveExtensionClient client) {
        this.client = client;
    }

    @Override
    public Mono<ListResult<PhotoGroup>> listPhotoGroup(QueryListRequest query) {
        var sort = Sort.by(
            Sort.Order.desc("spec.priority"),
            Sort.Order.desc("metadata.creationTimestamp"),
            Sort.Order.asc("metadata.name")
        );
        return this.client.listAll(PhotoGroup.class, toListOptions(query), Sort.unsorted())
            .sort((g1, g2) -> {
                var p1 = g1.getSpec() != null && g1.getSpec().getPriority() != null
                    ? g1.getSpec().getPriority() : 0;
                var p2 = g2.getSpec() != null && g2.getSpec().getPriority() != null
                    ? g2.getSpec().getPriority() : 0;
                int priorityCompare = Integer.compare(p2, p1);
                if (priorityCompare != 0) {
                    return priorityCompare;
                }
                var t1 = g1.getMetadata() != null ? g1.getMetadata().getCreationTimestamp() : null;
                var t2 = g2.getMetadata() != null ? g2.getMetadata().getCreationTimestamp() : null;
                if (t1 == null && t2 == null) {
                    return 0;
                }
                if (t1 == null) {
                    return 1;
                }
                if (t2 == null) {
                    return -1;
                }
                int timeCompare = t2.compareTo(t1);
                if (timeCompare != 0) {
                    return timeCompare;
                }
                var n1 = g1.getMetadata() != null ? g1.getMetadata().getName() : "";
                var n2 = g2.getMetadata() != null ? g2.getMetadata().getName() : "";
                return n1.compareTo(n2);
            })
            .collectList()
            .flatMap(groups -> {
                int page = Math.max(query.getPage(), 1);
                int size = query.getSize();
                int total = groups.size();
                int fromIndex = (page - 1) * size;
                boolean outOfBounds = fromIndex >= total;
                int safeFromIndex = outOfBounds ? 0 : fromIndex;
                int resultPage = outOfBounds ? 1 : page;
                int toIndex = Math.min(safeFromIndex + size, total);
                var pageGroups = groups.subList(safeFromIndex, toIndex);
                return Flux.fromIterable(pageGroups)
                    .flatMap(this::populatePhotos)
                    .collectList()
                    .map(populated -> new ListResult<>(resultPage, size, total, populated));
            });
    }

    @Override
    public Mono<PhotoGroup> deletePhotoGroup(String name) {
        return this.client.fetch(PhotoGroup.class, name)
            .flatMap(this.client::delete)
            .flatMap(deleted -> {
                    var listOptions = ListOptions.builder()
                        .andQuery(equal("spec.groupName", name))
                        .build();
                    return this.client.listAll(Photo.class, listOptions, Sort.unsorted())
                        .flatMap(this.client::delete)
                        .then()
                        .thenReturn(deleted);
                }
            );
    }

    private Mono<PhotoGroup> populatePhotos(PhotoGroup photoGroup) {
        return fetchPhotoCount(photoGroup)
            .doOnNext(count -> photoGroup.getStatusOrDefault().setPhotoCount(count))
            .thenReturn(photoGroup);
    }

    Mono<Integer> fetchPhotoCount(PhotoGroup photoGroup) {
        Assert.notNull(photoGroup, "The photoGroup must not be null.");
        String name = photoGroup.getMetadata().getName();
        return client.list(
                Photo.class,
                photo -> !photo.isDeleted()
                    && photo.getSpec() != null
                    && name.equals(photo.getSpec().getGroupName()),
                null
            )
            .count()
            .defaultIfEmpty(0L)
            .map(Long::intValue);
    }

    ListOptions toListOptions(QueryListRequest query) {
        return labelAndFieldSelectorToListOptions(
            query.getLabelSelector(), query.getFieldSelector()
        );
    }
}
