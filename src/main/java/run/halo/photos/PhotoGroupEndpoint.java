package run.halo.photos;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;

import lombok.AllArgsConstructor;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.router.IListRequest.QueryListRequest;
import run.halo.app.extension.router.QueryParamBuildUtil;
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
        final var tag = "api.plugin.halo.run/v1alpha1/PhotoGroup";
        return SpringdocRouteBuilder.route().GET(
            "plugins/PluginPhotos/photogroups", this::listPhotoGroup,
            builder -> {
                builder.operationId("ListPhotoGroups").description(
                    "List photoGroups.").tag(tag).response(
                    responseBuilder().implementation(
                        ListResult.generateGenericClass(PhotoGroup.class)));
                QueryParamBuildUtil.buildParametersFromType(builder,
                    QueryListRequest.class
                );
            }
        ).DELETE("plugins/PluginPhotos/photogroups/{name}",
            this::deletePhotoGroup, builder -> builder.operationId(
                    "DeletePhotoGroup")
                .description("Delete photoGroup.")
                .tag(tag)
                .response(responseBuilder().implementation(
                    ListResult.generateGenericClass(PhotoGroup.class)))
        ).build();
    }
    
    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("api.plugin.halo.run/v1alpha1");
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
