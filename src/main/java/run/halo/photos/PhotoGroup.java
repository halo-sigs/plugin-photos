package run.halo.photos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

/**
 * @author ryanwang
 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = "core.halo.run", version = "v1alpha1", kind = "PhotoGroup", plural = "photogroups", singular = "photogroup")
public class PhotoGroup extends AbstractExtension {
    
    @Schema(required = true) private PhotoGroupSpec spec;
    
    @Schema private PostGroupStatus status;
    
    @Data
    public static class PhotoGroupSpec {
        @Schema(required = true) private String displayName;
        
        private Integer priority;
    }
    
    @JsonIgnore
    public PostGroupStatus getStatusOrDefault() {
        if (this.status == null) {
            this.status = new PostGroupStatus();
        }
        return this.status;
    }
    
    @Data
    public static class PostGroupStatus {
        
        public Integer photoCount;
    }
}
