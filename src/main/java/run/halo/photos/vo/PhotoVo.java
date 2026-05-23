package run.halo.photos.vo;

import lombok.Builder;
import lombok.Value;
import run.halo.app.extension.MetadataOperator;
import run.halo.app.theme.finders.vo.ExtensionVoOperator;
import run.halo.photos.Photo;

/**
 * Value object exposed to themes for a single photo. GPS fields are stripped
 * from the EXIF data for privacy.
 *
 * @author LIlGG
 */
@Value
@Builder
public class PhotoVo implements ExtensionVoOperator {

    /**
     * Photo metadata (name, creationTimestamp, annotations, etc.).
     */
    MetadataOperator metadata;

    /**
     * Photo specification (displayName, url, cover, groupName, tags, etc.).
     */
    Photo.PhotoSpec spec;

    /**
     * EXIF metadata with GPS fields removed. May be null if the source image
     * had no EXIF data.
     */
    Photo.PhotoExif exif;

    /**
     * Permalink to the photo detail page, e.g. {@code /photos/my-photo-name}.
     */
    String permalink;

    /**
     * Build a {@link PhotoVo} from a {@link Photo}. GPS latitude, longitude and
     * altitude are intentionally cleared for privacy.
     *
     * @param photo the source photo
     * @return a value object safe for public theme consumption
     */
    public static PhotoVo from(Photo photo) {
        return PhotoVo.builder()
            .metadata(photo.getMetadata())
            .spec(photo.getSpec())
            .exif(cloneExifWithoutGps(photo.getExif()))
            .permalink("/photos/" + photo.getMetadata().getName())
            .build();
    }

    private static Photo.PhotoExif cloneExifWithoutGps(Photo.PhotoExif source) {
        if (source == null) {
            return null;
        }
        var copy = new Photo.PhotoExif();
        copy.setMake(source.getMake());
        copy.setModel(source.getModel());
        copy.setLensModel(source.getLensModel());
        copy.setSoftware(source.getSoftware());
        copy.setDateTimeOriginal(source.getDateTimeOriginal());
        copy.setFNumber(source.getFNumber());
        copy.setExposureTime(source.getExposureTime());
        copy.setIso(source.getIso());
        copy.setFocalLength(source.getFocalLength());
        copy.setFocalLengthIn35mm(source.getFocalLengthIn35mm());
        copy.setFlash(source.getFlash());
        copy.setWhiteBalance(source.getWhiteBalance());
        copy.setExposureMode(source.getExposureMode());
        copy.setExposureProgram(source.getExposureProgram());
        copy.setMeteringMode(source.getMeteringMode());
        copy.setImageWidth(source.getImageWidth());
        copy.setImageHeight(source.getImageHeight());
        // GPS fields intentionally nulled for privacy
        copy.setGpsLatitude(null);
        copy.setGpsLongitude(null);
        copy.setGpsAltitude(null);
        return copy;
    }
}
