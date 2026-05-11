package run.halo.photos;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.context.LazyContextVariable;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.index.query.Queries;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.app.theme.router.UrlContextListResult;
import run.halo.photos.finders.PhotoFinder;
import run.halo.photos.finders.PhotoPublicQueryService;
import run.halo.photos.vo.PhotoGroupVo;
import run.halo.photos.vo.PhotoVo;

/**
 * Provides theme-side routing for the photo gallery: a list page at <code>/photos</code>,
 * a detail page at <code>/photos/{name}</code>, and a 301 redirect for the legacy
 * <code>/photos/page/{page}</code> pagination URL.
 *
 * @author LIlGG
 */
@Component
@AllArgsConstructor
public class PhotoRouter {

    private static final String GROUP_PARAM = PhotoUrlBuilder.GROUP_PARAM;
    private static final String PAGE_PARAM = PhotoUrlBuilder.PAGE_PARAM;
    private static final String SIZE_PARAM = PhotoUrlBuilder.SIZE_PARAM;
    private static final int NEIGHBOR_WINDOW_SIZE = 5;
    private static final Duration BLOCKING_TIMEOUT = Duration.ofSeconds(10);

    private final PhotoFinder photoFinder;
    private final PhotoPublicQueryService photoPublicQueryService;
    private final ReactiveSettingFetcher settingFetcher;

    /**
     * Routes for the photo theme pages.
     *
     * <p>Route order is significant: the literal <code>/photos</code> and the legacy
     * <code>/photos/page/{page}</code> redirect are registered before the generic
     * <code>/photos/{name}</code> detail route so the legacy redirect is not shadowed.
     */
    @Bean
    RouterFunction<ServerResponse> photoRouter() {
        return route(GET("/photos"), listHandler())
            .andRoute(GET("/photos/page/{page:\\d+}"), legacyPaginationRedirect())
            .andRoute(GET("/photos/{name}"), detailHandler());
    }

    private HandlerFunction<ServerResponse> listHandler() {
        return request -> {
            var photoUrl = new PhotoUrlBuilder(request);
            String group = queryParam(request, GROUP_PARAM);
            int page = positiveInt(queryParam(request, PAGE_PARAM), 1);
            return resolvePageSize(queryParam(request, SIZE_PARAM)).flatMap(size -> {
                var photos = new LazyContextVariable<UrlContextListResult<PhotoVo>>() {
                    @Override
                    protected UrlContextListResult<PhotoVo> loadValue() {
                        System.out.println("进来了吗？");
                        return photoPublicQueryService.listPhotos(
                                buildListOptions(group),
                                PageRequestImpl.of(page, size, defaultPhotoSort()))
                            .map(list -> buildListContextResult(list, group, page, size))
                            .block(BLOCKING_TIMEOUT);
                    }
                };
                var groups = new LazyContextVariable<List<PhotoGroupVo>>() {
                    @Override
                    protected List<PhotoGroupVo> loadValue() {
                        return photoGroups().block(BLOCKING_TIMEOUT);
                    }
                };
                var title = new LazyContextVariable<String>() {
                    @Override
                    protected String loadValue() {
                        return getPhotosTitle().block(BLOCKING_TIMEOUT);
                    }
                };
                Map<String, Object> model = new HashMap<>();
                model.put("groups", groups);
                model.put("photos", photos);
                model.put(ModelConst.TEMPLATE_ID, "photos");
                model.put("title", title);
                model.put("photoUrl", photoUrl);
                return ServerResponse.ok().render("photos", model);
            });
        };
    }

    private HandlerFunction<ServerResponse> legacyPaginationRedirect() {
        return request -> {
            String page = request.pathVariables().get("page");
            var builder = UriComponentsBuilder.fromPath("/photos");
            if (StringUtils.isNotBlank(page)) {
                builder.queryParam(PAGE_PARAM, page);
            }
            request.queryParams().forEach((key, values) -> {
                if (PAGE_PARAM.equals(key)) {
                    return;
                }
                values.stream()
                    .filter(StringUtils::isNotBlank)
                    .forEach(value -> builder.queryParam(key, value));
            });
            URI location = URI.create(builder.build().toUriString());
            return ServerResponse.status(HttpStatus.MOVED_PERMANENTLY)
                .location(location)
                .build();
        };
    }

    private HandlerFunction<ServerResponse> detailHandler() {
        return request -> {
            String name = request.pathVariables().get("name");
            String group = queryParam(request, GROUP_PARAM);
            int page = positiveInt(queryParam(request, PAGE_PARAM), 1);
            return resolvePageSize(queryParam(request, SIZE_PARAM)).flatMap(size ->
                photoPublicQueryService.getByName(name)
                    .flatMap(photo -> renderOrRedirectDetail(request, photo, group, page, size))
            );
        };
    }

    private Mono<ServerResponse> renderOrRedirectDetail(ServerRequest request, PhotoVo photo,
        String group, int page, int size) {
        String photoGroup = photo.getSpec() == null ? null : photo.getSpec().getGroupName();
        if (StringUtils.isNotBlank(group) && !group.equals(photoGroup)) {
            URI redirectTo = stripQueryParam(request, GROUP_PARAM,
                "/photos/" + photo.getMetadata().getName());
            return ServerResponse.status(HttpStatus.FOUND).location(redirectTo).build();
        }
        return renderDetail(request, photo, group, page, size);
    }

    private Mono<ServerResponse> renderDetail(ServerRequest request, PhotoVo photo,
        String group, int page, int size) {
        String photoName = photo.getMetadata().getName();
        var photoUrl = new PhotoUrlBuilder(request);

        // Cache for the filtered photo context list to avoid duplicate queries
        AtomicReference<List<PhotoVo>> contextListRef = new AtomicReference<>();

        Supplier<List<PhotoVo>> resolveContextList = () -> {
            List<PhotoVo> cached = contextListRef.get();
            if (cached != null) {
                return cached;
            }
            List<PhotoVo> all = loadFilteredPhotos(group).block(BLOCKING_TIMEOUT);
            int currentIndex = indexOf(all, photoName);
            if (currentIndex < 0) {
                cached = List.of(photo);
            } else {
                cached = all;
            }
            contextListRef.compareAndSet(null, cached);
            return contextListRef.get();
        };

        var neighbors = new LazyContextVariable<List<PhotoVo>>() {
            @Override
            protected List<PhotoVo> loadValue() {
                System.out.println("neighbors 进来了吗？");
                List<PhotoVo> contextList = resolveContextList.get();
                int idx = indexOf(contextList, photoName);
                if (idx < 0) {
                    idx = 0;
                }
                int total = contextList.size();
                int start = windowStart(idx, total, NEIGHBOR_WINDOW_SIZE);
                int end = Math.min(start + NEIGHBOR_WINDOW_SIZE, total);
                List<PhotoVo> result = new ArrayList<>(end - start);
                for (int i = start; i < end; i++) {
                    result.add(contextList.get(i));
                }
                return result;
            }
        };

        var prev = new LazyContextVariable<PhotoVo>() {
            @Override
            protected PhotoVo loadValue() {
                List<PhotoVo> contextList = resolveContextList.get();
                int idx = indexOf(contextList, photoName);
                if (idx < 0) {
                    idx = 0;
                }
                return idx > 0 ? contextList.get(idx - 1) : null;
            }
        };

        var next = new LazyContextVariable<PhotoVo>() {
            @Override
            protected PhotoVo loadValue() {
                List<PhotoVo> contextList = resolveContextList.get();
                int idx = indexOf(contextList, photoName);
                if (idx < 0) {
                    idx = 0;
                }
                int total = contextList.size();
                return idx + 1 < total ? contextList.get(idx + 1) : null;
            }
        };

        var position = new LazyContextVariable<Integer>() {
            @Override
            protected Integer loadValue() {
                List<PhotoVo> contextList = resolveContextList.get();
                int idx = indexOf(contextList, photoName);
                if (idx < 0) {
                    idx = 0;
                }
                return idx + 1;
            }
        };

        var total = new LazyContextVariable<Integer>() {
            @Override
            protected Integer loadValue() {
                return resolveContextList.get().size();
            }
        };

        var title = new LazyContextVariable<String>() {
            @Override
            protected String loadValue() {
                return getDetailTitle(photo).block(BLOCKING_TIMEOUT);
            }
        };

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("photo", photo);
        model.put("neighbors", neighbors);
        model.put("prev", prev);
        model.put("next", next);
        model.put("position", position);
        model.put("total", total);
        model.put("group", group);
        model.put("page", page);
        model.put("size", size);
        model.put("backUrl", photoUrl.list(group, page, size));
        model.put("title", title);
        model.put(ModelConst.TEMPLATE_ID, "photo");
        model.put("photoUrl", photoUrl);
        return ServerResponse.ok().render("photo", model);
    }

    private Mono<List<PhotoVo>> loadFilteredPhotos(String group) {
        return photoPublicQueryService.listAllPhotos(
                buildListOptions(group),
                defaultPhotoSort())
            .collectList();
    }

    private static int indexOf(List<PhotoVo> photos, String name) {
        for (int i = 0; i < photos.size(); i++) {
            if (name.equals(photos.get(i).getMetadata().getName())) {
                return i;
            }
        }
        return -1;
    }

    static int windowStart(int currentIndex, int total, int windowSize) {
        if (total <= windowSize) {
            return 0;
        }
        int half = windowSize / 2;
        int start = currentIndex - half;
        if (start < 0) {
            return 0;
        }
        if (start + windowSize > total) {
            return total - windowSize;
        }
        return start;
    }

    private UrlContextListResult<PhotoVo> buildListContextResult(ListResult<PhotoVo> list,
        String group, int page, int size) {
        long total = list.getTotal();
        long totalPages = size > 0 ? (long) Math.ceil((double) total / size) : 1L;
        String prevUrl = page > 1 ? listPageUrl(group, page - 1, size) : "";
        String nextUrl = page < totalPages ? listPageUrl(group, page + 1, size) : "";
        return new UrlContextListResult.Builder<PhotoVo>()
            .listResult(list)
            .nextUrl(nextUrl)
            .prevUrl(prevUrl)
            .build();
    }

    private static String listPageUrl(String group, int page, int size) {
        var builder = UriComponentsBuilder.fromPath("/photos");
        if (page > 0) {
            builder.queryParam(PAGE_PARAM, page);
        }
        if (size > 0) {
            builder.queryParam(SIZE_PARAM, size);
        }
        if (StringUtils.isNotBlank(group)) {
            builder.queryParam(GROUP_PARAM, group);
        }
        return builder.build().toUriString();
    }

    private static URI stripQueryParam(ServerRequest request, String paramToStrip,
        String basePath) {
        var builder = UriComponentsBuilder.fromPath(basePath);
        request.queryParams().forEach((key, values) -> {
            if (paramToStrip.equals(key)) {
                return;
            }
            values.stream()
                .filter(StringUtils::isNotBlank)
                .forEach(value -> builder.queryParam(key, value));
        });
        return URI.create(builder.build().toUriString());
    }

    private static String queryParam(ServerRequest request, String name) {
        return request.queryParam(name)
            .filter(StringUtils::isNotBlank)
            .orElse(null);
    }

    private static int positiveInt(String value, int fallback) {
        int parsed = NumberUtils.toInt(value, fallback);
        return parsed > 0 ? parsed : fallback;
    }

    private Mono<Integer> resolvePageSize(String requested) {
        int requestedSize = NumberUtils.toInt(requested, -1);
        if (requestedSize > 0) {
            return Mono.just(requestedSize);
        }
        return this.settingFetcher.getSettingValue("base")
            .map(item -> item.get("pageSize").asInt(ModelConst.DEFAULT_PAGE_SIZE))
            .defaultIfEmpty(ModelConst.DEFAULT_PAGE_SIZE);
    }

    private Mono<String> getPhotosTitle() {
        return this.settingFetcher.getSettingValue("base")
            .map(setting -> setting.get("title").asText("图库"))
            .defaultIfEmpty("图库");
    }

    private Mono<String> getDetailTitle(PhotoVo photo) {
        String displayName = photo.getSpec() == null ? null : photo.getSpec().getDisplayName();
        if (StringUtils.isNotBlank(displayName)) {
            return Mono.just(displayName);
        }
        return getPhotosTitle();
    }

    private Mono<List<PhotoGroupVo>> photoGroups() {
        return photoFinder.groupBy().collectList();
    }

    private static Sort defaultPhotoSort() {
        return Sort.by(
            Sort.Order.desc("effectiveTime"),
            Sort.Order.desc("metadata.creationTimestamp"),
            Sort.Order.asc("metadata.name")
        );
    }

    private static ListOptions buildListOptions(String group) {
        var builder = ListOptions.builder();
        if (StringUtils.isNotBlank(group)) {
            builder.andQuery(Queries.equal("spec.groupName", group));
        }
        return builder.build();
    }
}
