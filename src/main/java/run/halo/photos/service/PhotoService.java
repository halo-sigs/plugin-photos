package run.halo.photos.service;

import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;
import run.halo.photos.Photo;
import run.halo.photos.PhotoQuery;

/**
 * A service for {@link Photo}.
 *
 * @author LIlGG
 * @since 1.0.0
 */
public interface PhotoService {
    
    /**
     * List photos.
     *
     * @param query query
     * @return a mono of list result
     */
    Mono<ListResult<Photo>> listPhoto(PhotoQuery query);
}
