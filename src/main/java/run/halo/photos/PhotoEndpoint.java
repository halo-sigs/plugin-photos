package run.halo.photos;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ListResult;
import run.halo.photos.service.PhotoService;

/**
 * A custom endpoint for {@link Photo}.
 *
 * @author LIlGG
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class PhotoEndpoint implements CustomEndpoint {

    private final PhotoService photoService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        final var tag = "console.api.photo.halo.run/v1alpha1/Photo";
        return route()
            .GET("photos", this::listPhoto,
                builder -> {
                    builder.operationId("ListPhotos")
                        .description("List photos.")
                        .tag(tag)
                        .response(responseBuilder().implementation(
                            ListResult.generateGenericClass(Photo.class)));

                    PhotoQuery.buildParameters(builder);
                }
            )
            .build();
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("console.api.photo.halo.run/v1alpha1");
    }

    private Mono<ServerResponse> listPhoto(ServerRequest serverRequest) {
        PhotoQuery query = new PhotoQuery(serverRequest.exchange());
        return photoService.listPhoto(query)
            .flatMap(photos -> ServerResponse.ok().bodyValue(photos));
    }

}
