package run.halo.photos.service;

import reactor.core.publisher.Flux;
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

    /**
     * List all distinct tags from photos.
     *
     * @param query query for filtering
     * @return a flux of distinct tag names
     */
    Flux<String> listAllTags(PhotoQuery query);

    /**
     * Delete a photo by name, optionally deleting its associated attachment.
     *
     * @param name           photo name
     * @param withAttachment when true, also deletes the {@code Attachment} whose
     *                       {@code status.permalink} matches {@code photo.spec.url}
     * @return the deleted photo, or empty if not found
     */
    Mono<Photo> deletePhoto(String name, boolean withAttachment);

    /**
     * Re-extract EXIF data from the local attachment file of an existing photo
     * and update the photo's {@code exif} field.
     *
     * @param name photo name
     * @return the updated photo; if no local attachment exists or the file cannot
     *         be read, returns the photo unchanged
     */
    Mono<Photo> reextractExif(String name);
}
