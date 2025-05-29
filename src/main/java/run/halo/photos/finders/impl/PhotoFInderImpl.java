package run.halo.photos.finders.impl;

import static org.springframework.data.domain.Sort.Order.asc;
import static org.springframework.data.domain.Sort.Order.desc;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.index.query.QueryFactory;
import run.halo.app.theme.finders.Finder;
import run.halo.photos.Photo;
import run.halo.photos.PhotoGroup;
import run.halo.photos.finders.PhotoFinder;
import run.halo.photos.vo.PhotoGroupVo;
import run.halo.photos.vo.PhotoVo;

/**
 * @author LIlGG
 */
@Finder("photoFinder")
public class PhotoFInderImpl implements PhotoFinder {
    private final ReactiveExtensionClient client;

    public PhotoFInderImpl(ReactiveExtensionClient client) {
        this.client = client;
    }

    @Override
    public Flux<PhotoVo> listAll() {
        return this.client.listAll(Photo.class, ListOptions.builder().build(), defaultSort())
            .map(PhotoVo::from);
    }

    @Override
    public Mono<ListResult<PhotoVo>> list(Integer page, Integer size) {
        return list(page, size, null);
    }

    @Override
    public Mono<ListResult<PhotoVo>> list(Integer page, Integer size, String group) {
        return pagePhoto(page, size, group);
    }

    private Mono<ListResult<PhotoVo>> pagePhoto(Integer page, Integer size, String group) {
        var builder = ListOptions.builder();
        if (StringUtils.isNotEmpty(group)) {
            builder.andQuery(QueryFactory.equal("spec.groupName", group));
        }
        return client.listBy(Photo.class, builder.build(),
                PageRequestImpl.of(page, size, defaultSort()))
            .flatMap(listResult -> Flux.fromStream(listResult.get())
                .map(PhotoVo::from)
                .collectList()
                .map(list -> new ListResult<>(
                    listResult.getPage(), listResult.getSize(), listResult.getTotal(), list
                ))
            );
    }

    @Override
    public Flux<PhotoVo> listBy(String groupName) {
        var options = ListOptions.builder()
            .andQuery(QueryFactory.equal("spec.groupName", groupName))
            .build();
        return client.listAll(Photo.class, options, defaultSort()).map(PhotoVo::from);
    }

    @Override
    public Flux<PhotoGroupVo> groupBy() {
        return this.client.listAll(PhotoGroup.class, ListOptions.builder().build(), defaultSort())
            .concatMap(group -> {
                var builder = PhotoGroupVo.from(group);
                return this.listBy(group.getMetadata().getName())
                    .collectList()
                    .doOnNext(photos -> {
                        PhotoGroup.PostGroupStatus status = group.getStatus();
                        status.setPhotoCount(photos.size());
                        builder.status(status);
                        builder.photos(photos);
                    })
                    .then(Mono.fromSupplier(builder::build));
            });
    }

    static Sort defaultSort() {
        return Sort.by(
            asc("spec.priority"),
            desc("metadata.creationTimestamp"),
            asc("metadata.name")
        );
    }

}
