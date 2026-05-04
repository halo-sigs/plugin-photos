package run.halo.photos;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

/**
 * @author ryanwang
 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = "core.halo.run", version = "v1alpha1", kind = "Photo", plural = "photos",
    singular = "photo")
public class Photo extends AbstractExtension {

    @Schema(requiredMode = REQUIRED)
    private PhotoSpec spec;

    private PhotoExif exif;

    @Data
    public static class PhotoSpec {
        @Schema(requiredMode = REQUIRED)
        private String displayName;

        private String description;

        @Schema(requiredMode = REQUIRED)
        private String url;

        private String cover;

        private Integer priority;

        @Schema(description = "Photo group name. Optional; empty or missing means the photo is "
            + "ungrouped.")
        private String groupName;

        private java.util.List<String> tags;
    }

    @Data
    public static class PhotoExif {
        // Camera info
        private String make;
        private String model;
        private String lensModel;
        private String software;

        // Shooting parameters
        private java.time.Instant dateTimeOriginal;
        private Double fNumber;
        private String exposureTime;
        private Integer iso;
        private Double focalLength;
        private Integer focalLengthIn35mm;
        private Integer flash;
        private Integer whiteBalance;
        private Integer exposureMode;
        private Integer exposureProgram;
        private Integer meteringMode;

        // Image dimensions
        private Integer imageWidth;
        private Integer imageHeight;

        // GPS location
        private Double gpsLatitude;
        private Double gpsLongitude;
        private Double gpsAltitude;
    }

    @JsonIgnore
    public boolean isDeleted() {
        return Objects.equals(true,
            getMetadata().getDeletionTimestamp() != null
        );
    }

}
