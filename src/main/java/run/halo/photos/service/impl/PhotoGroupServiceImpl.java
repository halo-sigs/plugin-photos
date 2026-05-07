package run.halo.photos.service.impl;

import static run.halo.app.extension.index.query.Queries.equal;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.attachment.Attachment;
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
    public Mono<PhotoGroup> deletePhotoGroup(String name, boolean deletePhotos,
        boolean withAttachment) {
        return this.client.fetch(PhotoGroup.class, name)
            .flatMap(group -> {
                var listOptions = ListOptions.builder()
                    .andQuery(equal("spec.groupName", name))
                    .build();
                Flux<Photo> photos = this.client.listAll(Photo.class, listOptions, Sort.unsorted());
                if (!deletePhotos) {
                    // Ungroup: clear spec.groupName so photos become ungrouped
                    return photos
                        .flatMap(photo -> {
                            photo.getSpec().setGroupName("");
                            return client.update(photo);
                        })
                        .then(this.client.delete(group))
                        .thenReturn(group);
                }
                if (withAttachment) {
                    // Delete photos and their attachment files
                    return photos
                        .flatMap(photo -> deletePhotoWithAttachment(photo))
                        .then(this.client.delete(group))
                        .thenReturn(group);
                }
                // Default: delete photos only
                return photos
                    .flatMap(this.client::delete)
                    .then(this.client.delete(group))
                    .thenReturn(group);
            });
    }

    private Mono<Photo> deletePhotoWithAttachment(Photo photo) {
        String url = photo.getSpec().getUrl();
        if (StringUtils.isBlank(url)) {
            return client.delete(photo);
        }
        var options = ListOptions.builder()
            .andQuery(equal("status.permalink", url))
            .build();
        return client.listAll(Attachment.class, options, Sort.unsorted())
            .flatMap(client::delete)
            .then(client.delete(photo));
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
