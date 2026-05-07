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
     * Delete a photo group by name.
     *
     * @param name           group name
     * @param deletePhotos   when {@code true}, all photos in the group are deleted;
     *                       when {@code false}, photos are unlinked (their
     *                       {@code spec.groupName} is cleared to empty string)
     * @param withAttachment when {@code true} (and {@code deletePhotos} is also {@code true}),
     *                       the {@code Attachment} matching each photo's {@code spec.url}
     *                       permalink is also deleted
     * @return the deleted group, or empty if not found
     */
    Mono<PhotoGroup> deletePhotoGroup(String name, boolean deletePhotos, boolean withAttachment);

}
