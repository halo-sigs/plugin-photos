package run.halo.photos.service.impl;

import static run.halo.app.extension.index.query.Queries.contains;
import static run.halo.app.extension.index.query.Queries.equal;
import static run.halo.app.extension.router.selector.SelectorUtil.labelAndFieldSelectorToListOptions;

import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.photos.Photo;
import run.halo.photos.PhotoQuery;
import run.halo.photos.PhotoSortUtils;
import run.halo.photos.service.PhotoService;

/**
 * Service implementation for {@link Photo}.
 *
 * @author LIlGG
 * @since 1.0.0
 */
@Component
class PhotoServiceImpl implements PhotoService {

    private final ReactiveExtensionClient client;

    public PhotoServiceImpl(ReactiveExtensionClient client) {
        this.client = client;
    }

    @Override
    public Mono<ListResult<Photo>> listPhoto(PhotoQuery query) {
        if (query.isEffectiveTimeSort()) {
            return listPhotoByEffectiveTime(query);
        }
        return this.client.listBy(
            Photo.class,
            toListOptions(query),
            PageRequestImpl.of(query.getPage(), query.getSize(), query.getSort())
        );
    }

    @Override
    public Flux<String> listAllTags(PhotoQuery query) {
        return client.listAll(Photo.class, toListOptions(query),
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

    private Mono<ListResult<Photo>> listPhotoByEffectiveTime(PhotoQuery query) {
        return client.listAll(Photo.class, toListOptions(query), Sort.unsorted())
            .sort(PhotoSortUtils.effectiveTimeComparator(query.isEffectiveTimeAscending()))
            .collectList()
            .map(photos -> new ListResult<>(
                query.getPage(),
                query.getSize(),
                photos.size(),
                ListResult.subList(photos, query.getPage(), query.getSize())
            ));
    }
}
