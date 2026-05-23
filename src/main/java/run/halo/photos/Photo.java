package run.halo.photos;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

/**
 * Photo extension resource. Registered as a custom resource under
 * {@code core.halo.run/v1alpha1} with kind {@code Photo}.
 *
 * <p>Each photo stores its display metadata in {@code spec} and optional EXIF
 * camera/shooting data in {@code exif}.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = "core.halo.run", version = "v1alpha1", kind = "Photo", plural = "photos",
    singular = "photo")
public class Photo extends AbstractExtension {

    /**
     * Photo specification containing display name, URL, group, tags, etc.
     */
    @Schema(requiredMode = REQUIRED)
    private PhotoSpec spec;

    /**
     * Optional EXIF metadata extracted from the image file.
     */
    private PhotoExif exif;

    /**
     * Specification fields for a {@link Photo}.
     */
    @Data
    @Schema(description = "Photo specification fields")
    public static class PhotoSpec {

        /**
         * Display name of the photo, shown in the gallery and theme pages.
         */
        @Schema(requiredMode = REQUIRED, description = "Display name of the photo")
        private String displayName;

        /**
         * Optional description or caption for the photo.
         */
        @Schema(description = "Optional description or caption")
        private String description;

        /**
         * Absolute URL of the photo image file. Usually a permalink to a Halo
         * Attachment or an external CDN link.
         */
        @Schema(requiredMode = REQUIRED,
            description = "Absolute URL of the photo image file")
        private String url;

        /**
         * Optional thumbnail / cover image URL. When absent, themes typically
         * fall back to {@code url}.
         */
        @Schema(description = "Optional thumbnail / cover image URL")
        private String cover;

        /**
         * Sort priority. Higher values appear first. Defaults to 0 when unset.
         */
        @Schema(description = "Sort priority (higher = earlier). Default 0.")
        private Integer priority;

        /**
         * Photo group name. Optional; empty or missing means the photo is
         * ungrouped.
         */
        @Schema(description = "Photo group name. Optional; empty or missing means the photo is "
            + "ungrouped.")
        private String groupName;

        /**
         * Optional list of tag strings attached to the photo.
         */
        @Schema(description = "Optional list of tag strings")
        private java.util.List<String> tags;
    }

    /**
     * EXIF metadata extracted from the image file. All fields are optional and
     * may be null when the source image lacks the corresponding EXIF tag.
     */
    @Data
    @Schema(description = "EXIF metadata extracted from the image file")
    public static class PhotoExif {

        // Camera info

        /**
         * Camera manufacturer, e.g. "Canon".
         */
        @Schema(description = "Camera manufacturer, e.g. 'Canon'")
        private String make;

        /**
         * Camera model name, e.g. "Canon EOS 5D Mark IV".
         */
        @Schema(description = "Camera model name")
        private String model;

        /**
         * Lens model name.
         */
        @Schema(description = "Lens model name")
        private String lensModel;

        /**
         * Software used to process the image, e.g. "Adobe Photoshop".
         */
        @Schema(description = "Software used to process the image")
        private String software;

        // Shooting parameters

        /**
         * Original capture time from EXIF DateTimeOriginal.
         */
        @Schema(description = "Original capture time from EXIF DateTimeOriginal")
        private java.time.Instant dateTimeOriginal;

        /**
         * Aperture f-number, e.g. 2.8.
         */
        @Schema(description = "Aperture f-number, e.g. 2.8")
        private Double fNumber;

        /**
         * Exposure time as a fraction string, e.g. "1/125".
         */
        @Schema(description = "Exposure time as a fraction string, e.g. '1/125'")
        private String exposureTime;

        /**
         * ISO sensitivity value.
         */
        @Schema(description = "ISO sensitivity value")
        private Integer iso;

        /**
         * Focal length in millimetres.
         */
        @Schema(description = "Focal length in millimetres")
        private Double focalLength;

        /**
         * Focal length converted to 35 mm equivalent.
         */
        @Schema(description = "Focal length in 35 mm equivalent")
        private Integer focalLengthIn35mm;

        /**
         * Flash firing status (EXIF flash tag value).
         */
        @Schema(description = "Flash firing status (EXIF flash tag value)")
        private Integer flash;

        /**
         * White balance mode.
         */
        @Schema(description = "White balance mode")
        private Integer whiteBalance;

        /**
         * Exposure mode.
         */
        @Schema(description = "Exposure mode")
        private Integer exposureMode;

        /**
         * Exposure program (auto, aperture-priority, etc.).
         */
        @Schema(description = "Exposure program")
        private Integer exposureProgram;

        /**
         * Metering mode.
         */
        @Schema(description = "Metering mode")
        private Integer meteringMode;

        // Image dimensions

        /**
         * Image width in pixels (EXIF ImageWidth).
         */
        @Schema(description = "Image width in pixels")
        private Integer imageWidth;

        /**
         * Image height in pixels (EXIF ImageHeight).
         */
        @Schema(description = "Image height in pixels")
        private Integer imageHeight;

        // GPS location

        /**
         * GPS latitude in decimal degrees. Hidden in public {@link run.halo.photos.vo.PhotoVo}
         * for privacy.
         */
        @Schema(description = "GPS latitude in decimal degrees")
        private Double gpsLatitude;

        /**
         * GPS longitude in decimal degrees. Hidden in public {@link run.halo.photos.vo.PhotoVo}
         * for privacy.
         */
        @Schema(description = "GPS longitude in decimal degrees")
        private Double gpsLongitude;

        /**
         * GPS altitude in metres. Hidden in public {@link run.halo.photos.vo.PhotoVo}
         * for privacy.
         */
        @Schema(description = "GPS altitude in metres")
        private Double gpsAltitude;
    }

    /**
     * Returns whether this photo is soft-deleted.
     *
     * @return true when metadata.deletionTimestamp is non-null
     */
    @JsonIgnore
    public boolean isDeleted() {
        return getMetadata().getDeletionTimestamp() != null;
    }

}
