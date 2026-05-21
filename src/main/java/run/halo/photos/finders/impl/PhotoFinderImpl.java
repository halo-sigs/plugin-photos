package run.halo.photos.finders.impl;

import static org.springframework.data.domain.Sort.Order.asc;
import static org.springframework.data.domain.Sort.Order.desc;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.index.query.Queries;
import run.halo.app.theme.finders.Finder;
import run.halo.photos.finders.PhotoFinder;
import run.halo.photos.finders.PhotoPublicQueryService;
import run.halo.photos.vo.PhotoGroupVo;
import run.halo.photos.vo.PhotoVo;

/**
 * Theme-side finder implementation for photos. Registered under the name
 * {@code photoFinder} and available in Thymeleaf templates.
 *
 * @author LIlGG
 */
@Finder("photoFinder")
public class PhotoFinderImpl implements PhotoFinder {

    private final PhotoPublicQueryService photoPublicQueryService;

    public PhotoFinderImpl(PhotoPublicQueryService photoPublicQueryService) {
        this.photoPublicQueryService = photoPublicQueryService;
    }

    /**
     * List all photos across all groups, sorted by effective time descending.
     *
     * @return a flux of all public photos
     */
    @Override
    public Flux<PhotoVo> listAll() {
        return photoPublicQueryService.listAllPhotos(ListOptions.builder().build(),
            defaultPhotoSort());
    }

    /**
     * List photos with pagination, optionally filtered by group.
     *
     * @param page page number (1-based)
     * @param size items per page
     * @return a mono of paginated photo list
     */
    @Override
    public Mono<ListResult<PhotoVo>> list(Integer page, Integer size) {
        return list(page, size, null);
    }

    /**
     * List photos with pagination, optionally filtered by group.
     *
     * @param page  page number (1-based)
     * @param size  items per page
     * @param group group name filter; null or empty means all groups
     * @return a mono of paginated photo list
     */
    @Override
    public Mono<ListResult<PhotoVo>> list(Integer page, Integer size, String group) {
        var options = ListOptions.builder();
        if (StringUtils.isNotEmpty(group)) {
            options.andQuery(Queries.equal("spec.groupName", group));
        }
        return photoPublicQueryService.listPhotos(options.build(),
            PageRequestImpl.of(page, size, defaultPhotoSort()));
    }

    /**
     * List all photos in a specific group.
     *
     * @param groupName the group name to filter by
     * @return a flux of photos in the group
     */
    @Override
    public Flux<PhotoVo> listBy(String groupName) {
        var options = ListOptions.builder()
            .andQuery(Queries.equal("spec.groupName", groupName))
            .build();
        return photoPublicQueryService.listAllPhotos(options, defaultPhotoSort());
    }

    /**
     * List all groups with their photos populated.
     *
     * @return a flux of groups, each containing its sorted photo list
     */
    @Override
    public Flux<PhotoGroupVo> groupBy() {
        return photoPublicQueryService.listGroups()
            .concatMap(group -> {
                String groupName = group.getMetadata().getName();
                return photoPublicQueryService.listAllPhotos(
                        ListOptions.builder()
                            .andQuery(Queries.equal("spec.groupName", groupName))
                            .build(),
                        defaultPhotoSort())
                    .collectList()
                    .map(photos -> PhotoGroupVo.builder()
                        .metadata(group.getMetadata())
                        .spec(group.getSpec())
                        .status(group.getStatus())
                        .photos(photos)
                        .build());
            });
    }

    static Sort defaultPhotoSort() {
        return Sort.by(
            desc("effectiveTime"),
            desc("metadata.creationTimestamp"),
            asc("metadata.name")
        );
    }
}
