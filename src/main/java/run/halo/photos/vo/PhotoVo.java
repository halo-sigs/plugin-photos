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
            .exif(photo.getExif())
            .permalink("/photos/" + photo.getMetadata().getName())
            .build();
    }
}
