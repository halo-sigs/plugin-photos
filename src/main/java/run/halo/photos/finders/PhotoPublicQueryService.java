package run.halo.photos.finders;

import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequest;
import run.halo.photos.Photo;
import run.halo.photos.vo.PhotoGroupVo;
import run.halo.photos.vo.PhotoTagVo;
import run.halo.photos.vo.PhotoVo;

/**
 * Single read-path for all public photo queries.
 */
public interface PhotoPublicQueryService {

    /**
     * List photos with filters and pagination.
     *
     * @param options list options
     * @param page    page request
     * @return a mono of list result
     */
    Mono<ListResult<PhotoVo>> listPhotos(ListOptions options, PageRequest page);

    /**
     * Get a single photo by name.
     *
     * @param name photo metadata name
     * @return a mono of photo vo, empty if missing or soft-deleted
     */
    Mono<PhotoVo> getByName(String name);

    /**
     * List photo groups without inline photos.
     *
     * @param options list options
     * @param page    page request
     * @return a mono of list result
     */
    Mono<ListResult<PhotoGroupVo>> listGroups(ListOptions options, PageRequest page);

    /**
     * List distinct tags with photo counts.
     *
     * @param nameFilter optional case-insensitive substring filter
     * @return a flux of tag vos
     */
    Flux<PhotoTagVo> listTags(String nameFilter);

    /**
     * Convert a {@link Photo} to {@link PhotoVo} with GPS-hidden transformation.
     *
     * @param photo the photo
     * @return a mono of photo vo
     */
    Mono<PhotoVo> toPhotoVo(Photo photo);

    /**
     * List all photos matching the given options, iterating through all pages.
     *
     * @param options list options
     * @param sort    sort order
     * @return a flux of all matching photo vos
     */
    Flux<PhotoVo> listAllPhotos(ListOptions options, Sort sort);

    /**
     * List all photo groups matching the given options, iterating through all pages.
     *
     * @param options list options
     * @param sort    sort order
     * @return a flux of all matching photo group vos
     */
    Flux<PhotoGroupVo> listAllGroups(ListOptions options, Sort sort);

    /**
     * Invalidate the tag cache so that the next call to {@link #listTags} re-fetches from DB.
     * Should be called whenever a {@link run.halo.photos.Photo} is added, updated, or deleted.
     */
    void invalidateTagCache();

}
