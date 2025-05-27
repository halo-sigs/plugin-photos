package run.halo.photos;

import static run.halo.app.extension.index.IndexAttributeFactory.simpleAttribute;

import org.springframework.stereotype.Component;
import run.halo.app.extension.SchemeManager;
import run.halo.app.extension.index.IndexSpec;
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
            indexSpecs.add(new IndexSpec()
                .setName("spec.groupName")
                .setIndexFunc(simpleAttribute(Photo.class, photo ->
                    photo.getSpec() == null ? "" : photo.getSpec().getGroupName()
                ))
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.displayName")
                .setIndexFunc(simpleAttribute(Photo.class, photo ->
                    photo.getSpec() == null ? "" : photo.getSpec().getDisplayName()
                ))
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.priority")
                .setIndexFunc(simpleAttribute(Photo.class, photo ->
                    photo.getSpec() == null || photo.getSpec().getPriority() == null
                        ? String.valueOf(0) : photo.getSpec().getPriority().toString()
                ))
            );
        });
        schemeManager.register(PhotoGroup.class, indexSpecs -> {
            indexSpecs.add(new IndexSpec()
                .setName("spec.priority")
                .setIndexFunc(simpleAttribute(PhotoGroup.class, group ->
                    group.getSpec() == null || group.getSpec().getPriority() == null
                        ? String.valueOf(0) : group.getSpec().getPriority().toString()
                ))
            );
        });
    }

    @Override
    public void stop() {
        schemeManager.unregister(schemeManager.get(Photo.class));
        schemeManager.unregister(schemeManager.get(PhotoGroup.class));
    }
}
