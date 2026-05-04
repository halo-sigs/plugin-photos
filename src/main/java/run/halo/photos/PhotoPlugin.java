package run.halo.photos;

import java.time.Instant;
import java.util.HashSet;
import org.springframework.stereotype.Component;
import run.halo.app.extension.SchemeManager;
import run.halo.app.extension.index.IndexSpecs;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

/**
 * @author ryanwang
 * @since 2.0.0
 */
@Component
public class PhotoPlugin extends BasePlugin {
    private final SchemeManager schemeManager;

    public PhotoPlugin(PluginContext pluginContext, SchemeManager schemeManager) {
        super(pluginContext);
        this.schemeManager = schemeManager;
    }

    @Override
    public void start() {
        schemeManager.register(Photo.class, indexSpecs -> {
            indexSpecs.add(IndexSpecs.<Photo, String>single("spec.groupName", String.class)
                .indexFunc(photo -> {
                    var groupName = photo.getSpec() == null
                        ? null : photo.getSpec().getGroupName();
                    return groupName == null ? "" : groupName;
                })
            );
            indexSpecs.add(IndexSpecs.<Photo, String>single("spec.displayName", String.class)
                .indexFunc(photo ->
                    photo.getSpec() == null ? "" : photo.getSpec().getDisplayName()
                )
            );
            indexSpecs.add(IndexSpecs.<Photo, String>single("spec.priority", String.class)
                .indexFunc(photo ->
                    photo.getSpec() == null || photo.getSpec().getPriority() == null
                        ? String.valueOf(0) : photo.getSpec().getPriority().toString()
                )
            );
            indexSpecs.add(IndexSpecs.<Photo, String>multi("spec.tags", String.class)
                .indexFunc(photo -> {
                    var tags = photo.getSpec() == null ? null : photo.getSpec().getTags();
                    return tags == null ? new HashSet<>() : new HashSet<>(tags);
                }));
            indexSpecs.add(IndexSpecs.<Photo, String>single("exif.make", String.class)
                .indexFunc(photo ->
                    photo.getExif() == null ? "" : photo.getExif().getMake() == null ? "" : photo.getExif().getMake()
                ));
            indexSpecs.add(IndexSpecs.<Photo, String>single("exif.model", String.class)
                .indexFunc(photo ->
                    photo.getExif() == null ? "" : photo.getExif().getModel() == null ? "" : photo.getExif().getModel()
                ));
            indexSpecs.add(IndexSpecs.<Photo, String>single("exif.dateTimeOriginal", String.class)
                .indexFunc(photo -> {
                    Instant dt = photo.getExif() == null ? null : photo.getExif().getDateTimeOriginal();
                    return dt == null ? "" : dt.toString();
                }));
        });
        schemeManager.register(PhotoGroup.class, indexSpecs -> {
            indexSpecs.add(IndexSpecs.<PhotoGroup, String>single("spec.priority", String.class)
                .indexFunc(group ->
                    group.getSpec() == null || group.getSpec().getPriority() == null
                        ? String.valueOf(0) : group.getSpec().getPriority().toString()
                )
            );
        });
    }

    @Override
    public void stop() {
        schemeManager.unregister(schemeManager.get(Photo.class));
        schemeManager.unregister(schemeManager.get(PhotoGroup.class));
    }
}
