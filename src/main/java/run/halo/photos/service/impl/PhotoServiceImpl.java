package run.halo.photos.service.impl;

import static run.halo.app.extension.router.selector.SelectorUtil.labelAndFieldSelectorToListOptions;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.index.query.QueryFactory;
import run.halo.photos.Photo;
import run.halo.photos.PhotoQuery;
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
        return this.client.listBy(
            Photo.class,
            toListOptions(query),
            PageRequestImpl.of(query.getPage(), query.getSize(), query.getSort())
        );
    }

    ListOptions toListOptions(PhotoQuery query) {
        var builder = ListOptions.builder(labelAndFieldSelectorToListOptions(
            query.getLabelSelector(), query.getFieldSelector())
        );

        if (StringUtils.isNotBlank(query.getKeyword())) {
            builder.andQuery(QueryFactory.contains("spec.displayName", query.getKeyword()));
        }
        if (StringUtils.isNotBlank(query.getGroup())) {
            builder.andQuery(QueryFactory.equal("spec.groupName", query.getGroup()));
        }
        return builder.build();
    }
}
