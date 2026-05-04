package run.halo.photos;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
import run.halo.photos.vo.PhotoTagVo;
import run.halo.photos.vo.PhotoVo;

/**
 * Public endpoint for photo queries.
 */
@Component
@RequiredArgsConstructor
public class PhotoQueryEndpoint implements CustomEndpoint {

    private final PhotoPublicQueryService photoPublicQueryService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        final var tag = "api.photo.halo.run/v1alpha1/Photo";
        return route()
            .GET("photos", this::listPhotos,
                builder -> {
                    builder.operationId("queryPhotos")
                        .description("List photos.")
                        .tag(tag)
                        .response(responseBuilder()
                            .implementation(ListResult.generateGenericClass(PhotoVo.class)));
                    PhotoPublicQuery.buildParameters(builder);
                }
            )
            .GET("photos/{name}", this::getPhoto,
                builder -> builder.operationId("queryPhotoByName")
                    .description("Get a photo by name.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .in(ParameterIn.PATH)
                        .name("name")
                        .description("Photo name")
                        .required(true))
                    .response(responseBuilder().implementation(PhotoVo.class))
            )
            .GET("tags", this::listTags,
                builder -> builder.operationId("queryPhotoTags")
                    .description("List photo tags with counts.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .in(ParameterIn.QUERY)
                        .name("name")
                        .description("Tag name filter")
                        .required(false)
                        .implementation(String.class))
                    .response(responseBuilder().implementationArray(PhotoTagVo.class))
            )
            .build();
    }

    private Mono<ServerResponse> listPhotos(ServerRequest request) {
        PhotoPublicQuery query = new PhotoPublicQuery(request.exchange());
        return photoPublicQueryService.listPhotos(query.toListOptions(), query.toPageRequest())
            .flatMap(result -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(result));
    }

    private Mono<ServerResponse> getPhoto(ServerRequest request) {
        String name = request.pathVariable("name");
        return photoPublicQueryService.getByName(name)
            .flatMap(photo -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(photo))
            .switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<ServerResponse> listTags(ServerRequest request) {
        String name = request.queryParam("name").orElse(null);
        return photoPublicQueryService.listTags(name)
            .collectList()
            .flatMap(tags -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(tags));
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("api.photo.halo.run/v1alpha1");
    }
}
