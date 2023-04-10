package run.halo.photos.service;

import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.router.IListRequest.QueryListRequest;
import run.halo.photos.PhotoGroup;

/**
 * A service for {@link PhotoGroup}.
 *
 * @author LIlGG
 * @since 2.0.0
 */
public interface PhotoGroupService {
    
    /**
     * List photo groups.
     *
     * @param request request
     * @return a mono of list result
     */
    Mono<ListResult<PhotoGroup>> listPhotoGroup(QueryListRequest request);
    
    /**
     * Create a photo group.
     *
     * @param name name
     * @return a mono of photo group
     */
    Mono<PhotoGroup> deletePhotoGroup(String name);
    
}
