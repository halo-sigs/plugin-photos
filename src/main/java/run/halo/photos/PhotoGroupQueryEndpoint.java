package run.halo.photos;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ListResult;
import run.halo.photos.finders.PhotoPublicQueryService;
import run.halo.photos.vo.PhotoGroupVo;

/**
 * Public endpoint for photo group queries.
 */
@Component
@RequiredArgsConstructor
public class PhotoGroupQueryEndpoint implements CustomEndpoint {

    private final PhotoPublicQueryService photoPublicQueryService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        final var tag = "api.photo.halo.run/v1alpha1/PhotoGroup";
        return route()
            .GET("photogroups", this::listGroups,
                builder -> builder.operationId("queryPhotoGroups")
                    .description("List photo groups.")
                    .tag(tag)
                    .response(responseBuilder()
                        .implementation(ListResult.generateGenericClass(PhotoGroupVo.class)))
            )
            .build();
    }

    private Mono<ServerResponse> listGroups(ServerRequest request) {
        PhotoPublicQuery query = new PhotoPublicQuery(request.exchange());
        return photoPublicQueryService.listGroups(query.toListOptions(), query.toPageRequest())
            .flatMap(result -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(result));
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("api.photo.halo.run/v1alpha1");
    }
}
