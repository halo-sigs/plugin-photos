package run.halo.photos.service.impl;

import static run.halo.app.extension.index.query.Queries.equal;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.photos.Photo;
import run.halo.photos.PhotoGroup;
import run.halo.photos.PhotoSortUtils;
import run.halo.photos.service.PhotoGroupService;

/**
 * Service implementation for {@link PhotoGroup}.
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
    public Mono<List<PhotoGroup>> listPhotoGroup() {
        return client.listAll(PhotoGroup.class, new ListOptions(), Sort.unsorted())
            .sort(PhotoSortUtils.groupComparator())
            .concatMap(this::populatePhotos)
            .collectList();
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
}
