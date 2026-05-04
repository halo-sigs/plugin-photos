package run.halo.photos.vo;

import lombok.Builder;
import lombok.Value;
import run.halo.app.extension.MetadataOperator;
import run.halo.app.theme.finders.vo.ExtensionVoOperator;
import run.halo.photos.Photo;

/**
 * @author LIlGG
 */
@Value
@Builder
public class PhotoVo implements ExtensionVoOperator {

    MetadataOperator metadata;

    Photo.PhotoSpec spec;

    Photo.PhotoExif exif;

    String permalink;

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
