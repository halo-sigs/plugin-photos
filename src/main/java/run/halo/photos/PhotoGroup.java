package run.halo.photos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

/**
 * @author ryanwang
 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = "core.halo.run", version = "v1alpha1", kind = "PhotoGroup",
    plural = "photogroups", singular = "photogroup")
public class PhotoGroup extends AbstractExtension {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private PhotoGroupSpec spec;

    @Schema
    private PhotoGroupStatus status;

    @Data
    public static class PhotoGroupSpec {
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        private String displayName;

        private Integer priority;
    }

    @JsonIgnore
    public PhotoGroupStatus getStatusOrDefault() {
        if (this.status == null) {
            this.status = new PhotoGroupStatus();
        }
        return this.status;
    }

    @Data
    public static class PhotoGroupStatus {

        public Integer photoCount;
    }
}
