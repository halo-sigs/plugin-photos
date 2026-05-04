package run.halo.photos.finders.impl;

import static run.halo.app.extension.index.query.Queries.contains;
import static run.halo.app.extension.index.query.Queries.equal;
import static run.halo.app.extension.router.selector.SelectorUtil.labelAndFieldSelectorToListOptions;

import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import run.halo.photos.PhotoPublicQuery;
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

    public PhotoPublicQueryServiceImpl(ReactiveExtensionClient client) {
        this.client = client;
    }

    @Override
    public Mono<ListResult<PhotoVo>> listPhotos(ListOptions options, PageRequest page) {
        var sort = page.getSort();
        boolean isEffectiveTimeSort = sort.isUnsorted()
            || (sort.stream().findFirst()
            .map(order -> PhotoPublicQuery.DATE_TIME_ORIGINAL_SORT.equals(order.getProperty()))
            .orElse(false));

        if (isEffectiveTimeSort) {
            boolean ascending = sort.stream()
                .filter(order -> PhotoPublicQuery.DATE_TIME_ORIGINAL_SORT.equals(order.getProperty()))
                .findFirst()
                .map(Sort.Order::isAscending)
                .orElse(false);
            return listPhotosByEffectiveTime(options, page, ascending);
        }

        return client.listBy(Photo.class, options, page)
            .flatMap(result -> Flux.fromIterable(result.getItems())
                .flatMap(this::toPhotoVo)
                .collectList()
                .map(items -> new ListResult<>(
                    result.getPage(), result.getSize(), result.getTotal(), items)));
    }

    private Mono<ListResult<PhotoVo>> listPhotosByEffectiveTime(
        ListOptions options, PageRequest page, boolean ascending) {
        return client.listAll(Photo.class, options, Sort.unsorted())
            .sort(PhotoSortUtils.effectiveTimeComparator(ascending))
            .flatMap(this::toPhotoVo)
            .collectList()
            .map(photos -> new ListResult<>(
                page.getPageNumber(), page.getPageSize(), photos.size(),
                ListResult.subList(photos, page.getPageNumber(), page.getPageSize())));
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
        return client.listAll(Photo.class,
                ListOptions.builder().build(), Sort.unsorted())
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
            });
    }

    @Override
    public Mono<PhotoVo> toPhotoVo(Photo photo) {
        return Mono.fromSupplier(() -> PhotoVo.from(photo));
    }

}
