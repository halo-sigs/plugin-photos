package run.halo.photos.finders.impl;

import static run.halo.app.extension.index.query.Queries.contains;
import static run.halo.app.extension.index.query.Queries.equal;
import static run.halo.app.extension.index.query.Queries.isNull;
import static run.halo.app.extension.index.query.Queries.not;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import static run.halo.app.extension.router.selector.SelectorUtil.labelAndFieldSelectorToListOptions;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequest;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.photos.Photo;
import run.halo.photos.PhotoGroup;
import run.halo.photos.PhotoSortUtils;
import run.halo.photos.finders.PhotoPublicQueryService;
import run.halo.photos.vo.PhotoGroupVo;
import run.halo.photos.vo.PhotoTagVo;
import run.halo.photos.vo.PhotoVo;

/**
 * Implementation of {@link PhotoPublicQueryService}.
 */
@Component
public class PhotoPublicQueryServiceImpl implements PhotoPublicQueryService {

    private final ReactiveExtensionClient client;

    private final Cache<String, List<PhotoTagVo>> tagCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(30))
        .build();

    public PhotoPublicQueryServiceImpl(ReactiveExtensionClient client) {
        this.client = client;
    }

    public void invalidateTagCache() {
        tagCache.invalidateAll();
    }

    @Override
    public Mono<ListResult<PhotoVo>> listPhotos(ListOptions options, PageRequest page) {
        return client.listBy(Photo.class, options, page)
            .flatMap(result -> Flux.fromIterable(result.getItems())
                .flatMap(this::toPhotoVo)
                .collectList()
                .map(items -> new ListResult<>(
                    result.getPage(), result.getSize(), result.getTotal(), items)));
    }

    @Override
    public Mono<PhotoVo> getByName(String name) {
        return client.fetch(Photo.class, name)
            .filter(photo -> !photo.isDeleted())
            .flatMap(this::toPhotoVo);
    }

    @Override
    public Mono<ListResult<PhotoGroupVo>> listGroups(ListOptions options, PageRequest page) {
        return client.listAll(PhotoGroup.class, options, Sort.unsorted())
            .sort(PhotoSortUtils.groupComparator())
            .collectList()
            .flatMap(groups -> {
                int total = groups.size();
                var pageItems = ListResult.subList(groups, page.getPageNumber(), page.getPageSize());
                return Flux.fromIterable(pageItems)
                    .concatMap(this::toGroupVo)
                    .collectList()
                    .map(items -> new ListResult<>(page.getPageNumber(), page.getPageSize(), total, items));
            });
    }

    private Mono<PhotoGroupVo> toGroupVo(PhotoGroup group) {
        return fetchPhotoCount(group)
            .map(count -> {
                var status = group.getStatusOrDefault();
                status.setPhotoCount(count);
                return PhotoGroupVo.builder()
                    .metadata(group.getMetadata())
                    .spec(group.getSpec())
                    .status(status)
                    .photos(null)
                    .build();
            });
    }

    private Mono<Integer> fetchPhotoCount(PhotoGroup group) {
        String name = group.getMetadata().getName();
        var options = ListOptions.builder()
            .andQuery(equal("spec.groupName", name))
            .build();
        return client.listBy(Photo.class, options,
                PageRequestImpl.of(1, 1, Sort.unsorted()))
            .map(ListResult::getTotal)
            .map(Long::intValue);
    }

    @Override
    public Flux<PhotoTagVo> listTags(String nameFilter) {
        var cacheKey = StringUtils.defaultString(nameFilter, "");
        var cached = tagCache.getIfPresent(cacheKey);
        if (cached != null) {
            return Flux.fromIterable(cached);
        }
        return client.listAll(Photo.class,
                ListOptions.builder().andQuery(not(isNull("spec.tags"))).build(),
                Sort.unsorted())
            .filter(photo -> !photo.isDeleted())
            .flatMapIterable(photo -> {
                var tags = photo.getSpec() == null ? null : photo.getSpec().getTags();
                return Objects.requireNonNullElseGet(tags, List::of);
            })
            .filter(tag -> StringUtils.isBlank(nameFilter)
                || StringUtils.containsIgnoreCase(tag, nameFilter))
            .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()))
            .flatMapMany(tagCounts -> {
                var entries = tagCounts.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> PhotoTagVo.builder()
                        .name(entry.getKey())
                        .photoCount(entry.getValue().intValue())
                        .build())
                    .toList();
                return Flux.fromIterable(entries);
            })
            .collectList()
            .doOnNext(list -> tagCache.put(cacheKey, list))
            .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Mono<PhotoVo> toPhotoVo(Photo photo) {
        return Mono.fromSupplier(() -> PhotoVo.from(photo));
    }

    @Override
    public Flux<PhotoVo> listAllPhotos(ListOptions options, Sort sort) {
        return fetchAllPages(
            page -> listPhotos(options, PageRequestImpl.of(page, 500, sort)));
    }

    @Override
    public Flux<PhotoGroupVo> listAllGroups(ListOptions options, Sort sort) {
        return fetchAllPages(
            page -> listGroups(options, PageRequestImpl.of(page, 500, sort)));
    }

    private static <T> Flux<T> fetchAllPages(
        Function<Integer, Mono<ListResult<T>>> fetchPage) {
        return fetchPage.apply(1)
            .expand(result -> {
                long totalPages = result.getSize() > 0
                    ? (long) Math.ceil((double) result.getTotal() / result.getSize())
                    : 0;
                if (result.getPage() < totalPages) {
                    return fetchPage.apply(result.getPage() + 1);
                }
                return Mono.empty();
            })
            .concatMapIterable(ListResult::getItems);
    }

}
