package run.halo.photos.service.impl;

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
import run.halo.app.extension.index.query.QueryFactory;
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
        return this.client.listBy(
                PhotoGroup.class,
                toListOptions(query),
                PageRequestImpl.of(query.getPage(), query.getSize())
            )
            .flatMap(listResult -> Flux.fromStream(listResult.get())
                .flatMap(this::populatePhotos)
                .collectList()
                .map(groups -> new ListResult<>(
                    listResult.getPage(),
                    listResult.getSize(),
                    listResult.getTotal(),
                    groups
                ))
            );
    }

    @Override
    public Mono<PhotoGroup> deletePhotoGroup(String name) {
        return this.client.fetch(PhotoGroup.class, name)
            .flatMap(this.client::delete)
            .flatMap(deleted -> {
                    var listOptions = ListOptions.builder()
                        .andQuery(QueryFactory.equal("spec.groupName", name))
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
                photo -> !photo.isDeleted() && photo.getSpec().getGroupName().equals(name),
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
