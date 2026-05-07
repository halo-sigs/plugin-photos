package run.halo.photos.service;

import java.util.List;
import reactor.core.publisher.Mono;
import run.halo.photos.PhotoGroup;

/**
 * A service for {@link PhotoGroup}.
 *
 * @author LIlGG
 * @since 2.0.0
 */
public interface PhotoGroupService {

    /**
     * List all photo groups sorted by priority.
     *
     * @return a mono of all photo groups with photo counts populated
     */
    Mono<List<PhotoGroup>> listPhotoGroup();
    
    /**
     * Create a photo group.
     *
     * @param name name
     * @return a mono of photo group
     */
    Mono<PhotoGroup> deletePhotoGroup(String name);
    
}
