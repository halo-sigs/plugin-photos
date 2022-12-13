package run.halo.photos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

import java.time.Instant;

/**
 * @author ryanwang
 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = "core.halo.run", version = "v1alpha1", kind = "Photo", plural = "photos", singular = "photo")
public class Photo extends AbstractExtension {

    private PhotoSpec spec;

    @Data
    public static class PhotoSpec {
        @Schema(required = true)
        private String displayName;

        private String description;

        @Schema(required = true)
        private String url;

        private String cover;

        private Integer priority;
    }
}
