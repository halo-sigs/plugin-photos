package run.halo.photos.finders.impl;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.comparator.Comparators;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
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
        return this.client.list(Photo.class, null, defaultPhotoComparator())
            .flatMap(photo -> Mono.just(PhotoVo.from(photo)));
    }
    
    @Override
    public Mono<ListResult<PhotoVo>> list(Integer page, Integer size) {
        return list(page, size, null);
    }
    
    @Override
    public Mono<ListResult<PhotoVo>> list(Integer page, Integer size,
        String group) {
        return pagePhoto(page, size, group, null, defaultPhotoComparator());
    }
    
    private Mono<ListResult<PhotoVo>> pagePhoto(Integer page, Integer size,
        String group, Predicate<Photo> photoPredicate,
        Comparator<Photo> comparator) {
        Predicate<Photo> predicate = photoPredicate == null ? photo -> true
            : photoPredicate;
        if (StringUtils.isNotEmpty(group)) {
            predicate = predicate.and(photo -> {
                String groupName = photo.getSpec().getGroupName();
                return StringUtils.equals(groupName, group);
            });
        }
        return client.list(Photo.class, predicate, comparator,
            pageNullSafe(page), sizeNullSafe(size)
        ).flatMap(list -> Flux.fromStream(list.get())
            .concatMap(photo -> Mono.just(PhotoVo.from(photo)))
            .collectList()
            .map(momentVos -> new ListResult<>(list.getPage(), list.getSize(),
                list.getTotal(), momentVos
            ))).defaultIfEmpty(new ListResult<>(page, size, 0L, List.of()));
    }
    
    @Override
    public Flux<PhotoVo> listBy(String groupName) {
        return client.list(Photo.class, photo -> {
            String group = photo.getSpec().getGroupName();
            return StringUtils.equals(group, groupName);
        }, defaultPhotoComparator()).flatMap(
            photo -> Mono.just(PhotoVo.from(photo)));
    }
    
    @Override
    public Flux<PhotoGroupVo> groupBy() {
        return this.client.list(PhotoGroup.class, null,
            defaultGroupComparator()
        ).concatMap(group -> {
            PhotoGroupVo.PhotoGroupVoBuilder builder = PhotoGroupVo.from(group);
            return this.listBy(group.getMetadata().getName()).collectList().map(
                photos -> {
                    PhotoGroup.PostGroupStatus status = group.getStatus();
                    status.setPhotoCount(photos.size());
                    builder.status(status);
                    builder.photos(photos);
                    return builder.build();
                });
        });
    }
    
    static Comparator<PhotoGroup> defaultGroupComparator() {
        Function<PhotoGroup, Integer> priority = group -> group.getSpec()
            .getPriority();
        Function<PhotoGroup, Instant> createTime = group -> group.getMetadata()
            .getCreationTimestamp();
        Function<PhotoGroup, String> name = group -> group.getMetadata()
            .getName();
        return Comparator.comparing(priority, Comparators.nullsLow())
            .thenComparing(createTime)
            .thenComparing(name);
    }
    
    static Comparator<Photo> defaultPhotoComparator() {
        Function<Photo, Integer> priority = link -> link.getSpec()
            .getPriority();
        Function<Photo, Instant> createTime = link -> link.getMetadata()
            .getCreationTimestamp();
        Function<Photo, String> name = link -> link.getMetadata().getName();
        return Comparator.comparing(priority, Comparators.nullsLow())
            .thenComparing(createTime)
            .thenComparing(name);
    }
    
    int pageNullSafe(Integer page) {
        return ObjectUtils.defaultIfNull(page, 1);
    }
    
    int sizeNullSafe(Integer size) {
        return ObjectUtils.defaultIfNull(size, 10);
    }
}
