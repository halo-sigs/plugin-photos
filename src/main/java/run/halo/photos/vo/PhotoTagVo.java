package run.halo.photos.vo;

import lombok.Builder;
import lombok.Value;

/**
 * A value object representing a photo tag with its usage count.
 *
 * @author ryanwang
 */
@Value
@Builder
public class PhotoTagVo {

    String name;

    Integer photoCount;

}
