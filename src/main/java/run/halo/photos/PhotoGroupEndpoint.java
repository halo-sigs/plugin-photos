package run.halo.photos;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
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
        final var tag = "console.api.photo.halo.run/v1alpha1/PhotoGroup";
        return route()
            .GET("photogroups", this::listPhotoGroup,
                builder -> builder.operationId("ListPhotoGroups")
                    .description("List all photo groups sorted by priority.")
                    .tag(tag)
                    .response(responseBuilder().implementationArray(PhotoGroup.class))
            )
            .DELETE("photogroups/{name}", this::deletePhotoGroup,
                builder -> builder.operationId("DeletePhotoGroup")
                    .description("Delete photo group.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("name")
                        .in(ParameterIn.PATH)
                        .description("Photo group name")
                        .implementation(String.class)
                        .required(true)
                    )
                    .parameter(parameterBuilder()
                        .name("deletePhotos")
                        .in(ParameterIn.QUERY)
                        .description("Delete photos in the group; when false, photos become "
                            + "ungrouped")
                        .required(false)
                        .implementation(Boolean.class)
                    )
                    .parameter(parameterBuilder()
                        .name("withAttachment")
                        .in(ParameterIn.QUERY)
                        .description("Also delete the attachment files of each photo "
                            + "(only effective when deletePhotos is true)")
                        .required(false)
                        .implementation(Boolean.class)
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
        boolean deletePhotos = serverRequest.queryParam("deletePhotos")
            .map(Boolean::parseBoolean)
            .orElse(true);
        boolean withAttachment = serverRequest.queryParam("withAttachment")
            .map(Boolean::parseBoolean)
            .orElse(false);
        return photoGroupService.deletePhotoGroup(name, deletePhotos, withAttachment)
            .flatMap(photoGroup -> ServerResponse.ok().bodyValue(photoGroup));
    }

    private Mono<ServerResponse> listPhotoGroup(ServerRequest serverRequest) {
        return photoGroupService.listPhotoGroup()
            .flatMap(groups -> ServerResponse.ok().bodyValue(groups));
    }

}
