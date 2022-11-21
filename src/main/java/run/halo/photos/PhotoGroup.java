package run.halo.photos;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

import java.util.LinkedHashSet;

/**
 * @author ryanwang
 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = "core.halo.run", version = "v1alpha1", kind = "PhotoGroup", plural = "photogroups", singular = "photogroup")
public class PhotoGroup extends AbstractExtension {

    private PhotoGroupSpec spec;

    @Data
    public static class PhotoGroupSpec {
        @Schema(required = true)
        private String displayName;

        private Integer priority;

        @Schema(description = "Names of photos below this group.")
        @ArraySchema(arraySchema = @Schema(description = "Photos of this group."), schema = @Schema(description = "Name of photo."))
        private LinkedHashSet<String> photos;
    }
}
