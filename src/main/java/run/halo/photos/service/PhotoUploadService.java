package run.halo.photos.service;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;
import run.halo.photos.Photo;

/**
 * Service for uploading photos directly to the gallery.
 *
 * @author ryanwang
 * @since 1.0.0
 */
public interface PhotoUploadService {

    /**
     * Upload a file to Halo attachment storage and create a Photo extension.
     *
     * @param filePart  the uploaded file
     * @param groupName the photo group name
     * @return the created Photo
     */
    Mono<Photo> upload(FilePart filePart, String groupName);
}
