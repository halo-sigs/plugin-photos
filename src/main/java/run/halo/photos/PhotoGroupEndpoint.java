package run.halo.photos;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.router.IListRequest.QueryListRequest;
import run.halo.photos.service.PhotoGroupService;

/**
 * A custom endpoint for {@link Photo}.
 *
 * @author LIlGG
 * @since 1.0.0
 */
@Component
@AllArgsConstructor
public class PhotoGroupEndpoint implements CustomEndpoint {

    private final PhotoGroupService photoGroupService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return RouterFunctions.route()
            .GET("photogroups", this::listPhotoGroup)
            .DELETE("photogroups/{name}", this::deletePhotoGroup)
            .build();
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("console.api.photo.halo.run/v1alpha1");
    }

    private Mono<ServerResponse> deletePhotoGroup(ServerRequest serverRequest) {
        String name = serverRequest.pathVariable("name");
        return photoGroupService.deletePhotoGroup(name).flatMap(
            photoGroup -> ServerResponse.ok().bodyValue(photoGroup));
    }

    private Mono<ServerResponse> listPhotoGroup(ServerRequest serverRequest) {
        QueryListRequest request = new PhotoQuery(serverRequest.queryParams());
        return photoGroupService.listPhotoGroup(request).flatMap(
            photoGroups -> ServerResponse.ok().bodyValue(photoGroups));
    }

}
