package run.halo.photos;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;
import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.fn.builders.parameter.Builder;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ListResult;
import run.halo.photos.service.PhotoService;
import run.halo.photos.service.PhotoUploadService;

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
    private final PhotoUploadService photoUploadService;

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
            .GET("photos/tags", this::listTags,
                builder -> builder.operationId("ListPhotoTags")
                    .description("List all photo tags.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .in(ParameterIn.QUERY)
                        .name("name")
                        .description("Tag name to query")
                        .required(false)
                        .implementation(String.class))
                    .response(responseBuilder().implementationArray(String.class))
            )
            .POST("photos/upload", this::uploadPhoto,
                builder -> builder.operationId("UploadPhoto")
                    .description("Upload a photo directly to the gallery. The `group` field "
                        + "SHOULD be sent as a multipart form field; for backward compatibility "
                        + "it is also accepted as a query parameter.")
                    .tag(tag)
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.MULTIPART_FORM_DATA_VALUE)
                            .schema(schemaBuilder().implementation(UploadPhotoRequest.class))))
                    .response(responseBuilder().implementation(Photo.class))
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

    private Mono<ServerResponse> uploadPhoto(ServerRequest request) {
        var queryGroup = request.queryParam("group").orElse("");
        return request.multipartData()
            .flatMap(parts -> {
                var filePart = parts.getFirst("file");
                if (filePart == null) {
                    return Mono.error(new IllegalArgumentException("File is required"));
                }
                var group = resolveGroup(parts.getFirst("group"), queryGroup);
                return photoUploadService.upload((FilePart) filePart, group);
            })
            .flatMap(photo -> ServerResponse.ok().bodyValue(photo));
    }

    private static String resolveGroup(Part groupPart, String queryGroup) {
        if (groupPart instanceof FormFieldPart formFieldPart) {
            var value = formFieldPart.value();
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return StringUtils.isNotBlank(queryGroup) ? queryGroup : "";
    }

    private Mono<ServerResponse> listTags(ServerRequest request) {
        String name = request.queryParam("name").orElse(null);
        PhotoQuery query = new PhotoQuery(request.exchange());
        return photoService.listAllTags(query)
            .filter(tagName -> StringUtils.isBlank(name) || StringUtils.containsIgnoreCase(tagName, name))
            .collectList()
            .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    @Schema(name = "PhotoUploadRequest")
    record UploadPhotoRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
            type = "string", format = "binary",
            description = "Image file to upload")
        Object file,

        @Schema(description = "Photo group name to assign the new photo to")
        String group
    ) {
    }

}
