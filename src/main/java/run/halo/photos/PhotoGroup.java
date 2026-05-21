package run.halo.photos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

/**
 * Photo group extension resource. Registered as a custom resource under
 * {@code core.halo.run/v1alpha1} with kind {@code PhotoGroup}.
 *
 * <p>Groups let photos be organised into named collections (e.g. albums).
 * Deleting a group can either cascade-delete its photos or simply ungroup them.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = "core.halo.run", version = "v1alpha1", kind = "PhotoGroup",
    plural = "photogroups", singular = "photogroup")
public class PhotoGroup extends AbstractExtension {

    /**
     * Group specification containing display name and sort priority.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
        description = "Group specification")
    private PhotoGroupSpec spec;

    /**
     * Computed status, currently holding the photo count in the group.
     */
    @Schema(description = "Computed group status")
    private PhotoGroupStatus status;

    /**
     * Specification fields for a {@link PhotoGroup}.
     */
    @Data
    @Schema(description = "PhotoGroup specification fields")
    public static class PhotoGroupSpec {

        /**
         * Human-readable display name of the group.
         */
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
            description = "Human-readable display name of the group")
        private String displayName;

        /**
         * Sort priority. Higher values appear first. Defaults to 0 when unset.
         */
        @Schema(description = "Sort priority (higher = earlier). Default 0.")
        private Integer priority;
    }

    /**
     * Returns the current status, creating a default empty one if absent.
     *
     * @return non-null status instance
     */
    @JsonIgnore
    public PhotoGroupStatus getStatusOrDefault() {
        if (this.status == null) {
            this.status = new PhotoGroupStatus();
        }
        return this.status;
    }

    /**
     * Computed status fields for a {@link PhotoGroup}.
     */
    @Data
    @Schema(description = "Computed status fields for PhotoGroup")
    public static class PhotoGroupStatus {

        /**
         * Number of photos currently assigned to this group.
         */
        @Schema(description = "Number of photos in this group")
        public Integer photoCount;
    }
}
