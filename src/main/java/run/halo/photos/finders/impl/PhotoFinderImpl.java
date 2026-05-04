package run.halo.photos.finders.impl;

import static org.springframework.data.domain.Sort.Order.asc;
import static org.springframework.data.domain.Sort.Order.desc;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.index.query.Queries;
import run.halo.app.theme.finders.Finder;
import run.halo.photos.PhotoGroup;
import run.halo.photos.finders.PhotoFinder;
import run.halo.photos.finders.PhotoPublicQueryService;
import run.halo.photos.vo.PhotoGroupVo;
import run.halo.photos.vo.PhotoVo;

/**
 * @author LIlGG
 */
@Finder("photoFinder")
public class PhotoFinderImpl implements PhotoFinder {

    private final PhotoPublicQueryService photoPublicQueryService;

    public PhotoFinderImpl(PhotoPublicQueryService photoPublicQueryService) {
        this.photoPublicQueryService = photoPublicQueryService;
    }

    @Override
    public Flux<PhotoVo> listAll() {
        return photoPublicQueryService.listPhotos(ListOptions.builder().build(),
                PageRequestImpl.of(1, Integer.MAX_VALUE, defaultPhotoSort()))
            .flatMapIterable(ListResult::getItems);
    }

    @Override
    public Mono<ListResult<PhotoVo>> list(Integer page, Integer size) {
        return list(page, size, null);
    }

    @Override
    public Mono<ListResult<PhotoVo>> list(Integer page, Integer size, String group) {
        var options = ListOptions.builder();
        if (StringUtils.isNotEmpty(group)) {
            options.andQuery(Queries.equal("spec.groupName", group));
        }
        return photoPublicQueryService.listPhotos(options.build(),
            PageRequestImpl.of(page, size, defaultPhotoSort()));
    }

    @Override
    public Flux<PhotoVo> listBy(String groupName) {
        var options = ListOptions.builder()
            .andQuery(Queries.equal("spec.groupName", groupName))
            .build();
        return photoPublicQueryService.listPhotos(options,
                PageRequestImpl.of(1, Integer.MAX_VALUE, defaultPhotoSort()))
            .flatMapIterable(ListResult::getItems);
    }

    @Override
    public Flux<PhotoGroupVo> groupBy() {
        return photoPublicQueryService.listGroups(ListOptions.builder().build(),
                PageRequestImpl.of(1, Integer.MAX_VALUE, Sort.unsorted()))
            .flatMapIterable(ListResult::getItems)
            .concatMap(group -> {
                String groupName = group.getMetadata().getName();
                return photoPublicQueryService.listPhotos(
                        ListOptions.builder()
                            .andQuery(Queries.equal("spec.groupName", groupName))
                            .build(),
                        PageRequestImpl.of(1, Integer.MAX_VALUE, defaultPhotoSort()))
                    .map(photos -> PhotoGroupVo.builder()
                        .metadata(group.getMetadata())
                        .spec(group.getSpec())
                        .status(group.getStatus())
                        .photos(photos.getItems())
                        .build());
            });
    }

    static Sort defaultPhotoSort() {
        return Sort.by(
            desc("exif.dateTimeOriginal"),
            desc("metadata.creationTimestamp"),
            asc("metadata.name")
        );
    }
}
