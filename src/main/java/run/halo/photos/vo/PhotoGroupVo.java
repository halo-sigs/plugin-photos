package run.halo.photos.vo;

import lombok.Builder;
import lombok.Value;
import run.halo.app.extension.MetadataOperator;
import run.halo.app.theme.finders.vo.ExtensionVoOperator;
import run.halo.photos.PhotoGroup;

import java.util.List;

/**
 * @author LIlGG
 */
@Value
@Builder
public class PhotoGroupVo implements ExtensionVoOperator {
    MetadataOperator metadata;
    
    PhotoGroup.PhotoGroupSpec spec;
    
    PhotoGroup.PostGroupStatus status;
    
    List<PhotoVo> photos;
    
    public static PhotoGroupVoBuilder from(PhotoGroup photoGroup) {
        return PhotoGroupVo.builder()
            .metadata(photoGroup.getMetadata())
            .spec(photoGroup.getSpec())
            .status(photoGroup.getStatusOrDefault())
            .photos(List.of());
    }
}
