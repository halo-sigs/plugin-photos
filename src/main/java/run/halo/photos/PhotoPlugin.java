package run.halo.photos;

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
                .indexFunc(photo ->
                    photo.getSpec() == null ? "" : photo.getSpec().getGroupName()
                )
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
