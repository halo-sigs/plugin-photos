package run.halo.photos;

import org.springframework.stereotype.Component;
import run.halo.app.extension.SchemeManager;
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
        schemeManager.register(Photo.class);
        schemeManager.register(PhotoGroup.class);
    }

    @Override
    public void stop() {
        schemeManager.unregister(schemeManager.get(Photo.class));
        schemeManager.unregister(schemeManager.get(PhotoGroup.class));
    }
}
