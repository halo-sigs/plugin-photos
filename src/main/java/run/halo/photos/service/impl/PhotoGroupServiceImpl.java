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
        return this.client.listAll(PhotoGroup.class, toListOptions(query), Sort.unsorted())
            .sort(run.halo.photos.PhotoSortUtils.groupComparator())
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
            .flatMap(group -> {
                var listOptions = ListOptions.builder()
                    .andQuery(equal("spec.groupName", name))
                    .build();
                return this.client.listAll(Photo.class, listOptions, Sort.unsorted())
                    .flatMap(this.client::delete)
                    .then(this.client.delete(group))
                    .thenReturn(group);
            });
    }

    private Mono<PhotoGroup> populatePhotos(PhotoGroup photoGroup) {
        return fetchPhotoCount(photoGroup)
            .doOnNext(count -> photoGroup.getStatusOrDefault().setPhotoCount(count))
            .thenReturn(photoGroup);
    }

    Mono<Integer> fetchPhotoCount(PhotoGroup photoGroup) {
        Assert.notNull(photoGroup, "The photoGroup must not be null.");
        String name = photoGroup.getMetadata().getName();
        var options = ListOptions.builder()
            .andQuery(equal("spec.groupName", name))
            .build();
        return client.listBy(Photo.class, options,
                PageRequestImpl.of(1, 1, Sort.unsorted()))
            .map(ListResult::getTotal)
            .map(Long::intValue);
    }

    ListOptions toListOptions(QueryListRequest query) {
        return labelAndFieldSelectorToListOptions(
            query.getLabelSelector(), query.getFieldSelector()
        );
    }
}
