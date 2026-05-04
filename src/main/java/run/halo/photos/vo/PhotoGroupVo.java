package run.halo.photos.vo;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import run.halo.app.extension.MetadataOperator;
import run.halo.app.theme.finders.vo.ExtensionVoOperator;
import run.halo.photos.PhotoGroup;

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
}
