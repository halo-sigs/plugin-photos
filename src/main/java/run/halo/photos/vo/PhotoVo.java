package run.halo.photos.vo;

import lombok.Builder;
import lombok.Value;
import run.halo.app.extension.MetadataOperator;
import run.halo.photos.Photo;

/**
 * @author LIlGG
 */
@Value
@Builder
public class PhotoVo {
    
    MetadataOperator metadata;
    
    Photo.PhotoSpec spec;
    
    public static PhotoVo from(Photo photo) {
        return PhotoVo.builder()
            .metadata(photo.getMetadata())
            .spec(photo.getSpec())
            .build();
    }
}
