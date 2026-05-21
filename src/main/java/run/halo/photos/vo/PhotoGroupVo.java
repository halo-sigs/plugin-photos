package run.halo.photos.vo;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import run.halo.app.extension.MetadataOperator;
import run.halo.app.theme.finders.vo.ExtensionVoOperator;
import run.halo.photos.PhotoGroup;

/**
 * Value object exposed to themes for a photo group, optionally including the
 * list of photos belonging to the group.
 *
 * @author LIlGG
 */
@Value
@Builder
public class PhotoGroupVo implements ExtensionVoOperator {

    /**
     * Group metadata (name, creationTimestamp, etc.).
     */
    MetadataOperator metadata;

    /**
     * Group specification (displayName, priority).
     */
    PhotoGroup.PhotoGroupSpec spec;

    /**
     * Computed status, e.g. photo count in the group.
     */
    PhotoGroup.PhotoGroupStatus status;

    /**
     * Photos in this group. May be null when the caller only requested group
     * summaries without populating photos.
     */
    List<PhotoVo> photos;
}
