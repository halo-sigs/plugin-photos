package run.halo.photos;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.router.IListRequest.QueryListRequest;
import run.halo.photos.service.PhotoGroupService;

/**
 * A custom endpoint for {@link Photo}.
 *
 * @author LIlGG
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class PhotoGroupEndpoint implements CustomEndpoint {

    private final PhotoGroupService photoGroupService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return route()
            .GET("photogroups", this::listPhotoGroup,
                builder -> {
                    builder.operationId("ListPhotos")
                        .description("List photos.")
                        .response(responseBuilder().implementation(
                            ListResult.generateGenericClass(PhotoGroup.class))
                        );

                    PhotoQuery.buildParameters(builder);
                }
            )
            .DELETE("photogroups/{name}", this::deletePhotoGroup,
                builder -> builder.operationId("DeletePhotoGroup")
                    .description("Delete photo group.")
                    .parameter(parameterBuilder()
                        .name("name")
                        .in(ParameterIn.PATH)
                        .description("Photo group name")
                        .implementation(String.class)
                        .required(true)
                    )
                    .response(responseBuilder().implementation(PhotoGroup.class))
            )
            .build();
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("console.api.photo.halo.run/v1alpha1");
    }

    private Mono<ServerResponse> deletePhotoGroup(ServerRequest serverRequest) {
        String name = serverRequest.pathVariable("name");
        if (StringUtils.isBlank(name)) {
            throw new ServerWebInputException("Photo group name must not be blank.");
        }
        return photoGroupService.deletePhotoGroup(name)
            .flatMap(photoGroup -> ServerResponse.ok().bodyValue(photoGroup));
    }

    private Mono<ServerResponse> listPhotoGroup(ServerRequest serverRequest) {
        QueryListRequest request = new PhotoQuery(serverRequest.queryParams());
        return photoGroupService.listPhotoGroup(request)
            .flatMap(photoGroups -> ServerResponse.ok().bodyValue(photoGroups));
    }

}
