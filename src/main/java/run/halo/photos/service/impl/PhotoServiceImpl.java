package run.halo.photos.service.impl;

import static run.halo.app.extension.index.query.Queries.contains;
import static run.halo.app.extension.index.query.Queries.equal;
import static run.halo.app.extension.index.query.Queries.isNull;
import static run.halo.app.extension.index.query.Queries.not;
import static run.halo.app.extension.router.selector.SelectorUtil.labelAndFieldSelectorToListOptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.attachment.Attachment;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.infra.BackupRootGetter;
import run.halo.photos.Photo;
import run.halo.photos.PhotoQuery;
import run.halo.photos.service.PhotoService;

/**
 * Service implementation for {@link Photo}.
 *
 * @author LIlGG
 * @since 1.0.0
 */
@Slf4j
@Component
class PhotoServiceImpl implements PhotoService {

    private final ReactiveExtensionClient client;
    private final BackupRootGetter backupRootGetter;
    private final ExifExtractor exifExtractor;

    public PhotoServiceImpl(ReactiveExtensionClient client,
        BackupRootGetter backupRootGetter,
        ExifExtractor exifExtractor) {
        this.client = client;
        this.backupRootGetter = backupRootGetter;
        this.exifExtractor = exifExtractor;
    }

    @Override
    public Mono<ListResult<Photo>> listPhoto(PhotoQuery query) {
        return this.client.listBy(
            Photo.class,
            toListOptions(query),
            PageRequestImpl.of(query.getPage(), query.getSize(), query.getSort())
        );
    }

    @Override
    public Flux<String> listAllTags(PhotoQuery query) {
        var options = ListOptions.builder(toListOptions(query))
            .andQuery(not(isNull("spec.tags")))
            .build();
        return client.listAll(Photo.class, options,
                Sort.by("metadata.name").descending())
            .flatMapIterable(photo -> {
                var tags = photo.getSpec() == null ? null : photo.getSpec().getTags();
                return Objects.requireNonNullElseGet(tags, List::of);
            })
            .distinct();
    }

    ListOptions toListOptions(PhotoQuery query) {
        var builder = ListOptions.builder(labelAndFieldSelectorToListOptions(
            query.getLabelSelector(), query.getFieldSelector())
        );

        if (StringUtils.isNotBlank(query.getKeyword())) {
            builder.andQuery(contains("spec.displayName", query.getKeyword()));
        }
        if (query.isUngrouped()) {
            // Match photos whose groupName is the empty string. The index function in
            // PhotoPlugin already coerces null to "" so this also matches null values.
            builder.andQuery(equal("spec.groupName", ""));
        } else if (StringUtils.isNotBlank(query.getGroup())) {
            builder.andQuery(equal("spec.groupName", query.getGroup()));
        }
        if (StringUtils.isNotBlank(query.getTag())) {
            builder.andQuery(equal("spec.tags", query.getTag()));
        }
        return builder.build();
    }

    @Override
    public Mono<Photo> deletePhoto(String name, boolean withAttachment) {
        return client.fetch(Photo.class, name)
            .flatMap(photo -> {
                if (withAttachment && StringUtils.isNotBlank(photo.getSpec().getUrl())) {
                    var options = ListOptions.builder()
                        .andQuery(equal("status.permalink", photo.getSpec().getUrl()))
                        .build();
                    return client.listAll(Attachment.class, options, Sort.unsorted())
                        .flatMap(client::delete)
                        .then(client.delete(photo));
                }
                return client.delete(photo);
            });
    }

    @Override
    public Mono<Photo> reextractExif(String name) {
        return client.fetch(Photo.class, name)
            .flatMap(photo -> {
                var url = photo.getSpec().getUrl();
                if (StringUtils.isBlank(url)) {
                    return Mono.just(photo);
                }
                var options = ListOptions.builder()
                    .andQuery(equal("status.permalink", url))
                    .build();
                return client.listAll(Attachment.class, options, Sort.unsorted())
                    .next()
                    .flatMap(attachment -> {
                        try {
                            var workDir = backupRootGetter.get().getParent();
                            var filePath = workDir.resolve("attachments" + url);
                            if (!Files.exists(filePath)) {
                                log.warn("Attachment file not found for photo {}: {}", name, filePath);
                                return Mono.just(photo);
                            }
                            var bytes = Files.readAllBytes(filePath);
                            var exifData = exifExtractor.extractExif(bytes);
                            var exif = exifExtractor.toPhotoExif(exifData);
                            photo.setExif(exif);
                            return client.update(photo);
                        } catch (Exception e) {
                            log.warn("Failed to re-extract EXIF for photo {}: {}", name, e.getMessage());
                            return Mono.just(photo);
                        }
                    })
                    .switchIfEmpty(Mono.just(photo));
            });
    }
}
