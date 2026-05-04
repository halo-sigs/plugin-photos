package run.halo.photos;

import java.time.Instant;
import java.util.HashSet;
import org.springframework.stereotype.Component;
import run.halo.app.extension.Extension;
import run.halo.app.extension.Scheme;
import run.halo.app.extension.SchemeManager;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.Watcher;
import run.halo.app.extension.index.IndexSpecs;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;
import run.halo.photos.finders.impl.PhotoPublicQueryServiceImpl;

/**
 * @author ryanwang
 * @since 2.0.0
 */
@Component
public class PhotoPlugin extends BasePlugin {
    private final SchemeManager schemeManager;
    private final ReactiveExtensionClient client;
    private final PhotoPublicQueryServiceImpl photoPublicQueryService;
    private Watcher photoWatcher;

    public PhotoPlugin(PluginContext pluginContext, SchemeManager schemeManager,
        ReactiveExtensionClient client,
        PhotoPublicQueryServiceImpl photoPublicQueryService) {
        super(pluginContext);
        this.schemeManager = schemeManager;
        this.client = client;
        this.photoPublicQueryService = photoPublicQueryService;
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
            indexSpecs.add(IndexSpecs.<Photo, String>single("effectiveTime", String.class)
                .indexFunc(PhotoPlugin::computeEffectiveTimeIndex));
        });
        schemeManager.register(PhotoGroup.class, indexSpecs -> {
            indexSpecs.add(IndexSpecs.<PhotoGroup, Integer>single("spec.priority", Integer.class)
                .indexFunc(group ->
                    group.getSpec() == null || group.getSpec().getPriority() == null
                        ? 0 : group.getSpec().getPriority()
                )
            );
        });
        photoWatcher = new Watcher() {
            private volatile boolean disposed = false;

            @Override
            public void onAdd(Extension extension) {
                if (extension instanceof Photo) {
                    photoPublicQueryService.invalidateTagCache();
                }
            }

            @Override
            public void onUpdate(Extension oldObj, Extension newObj) {
                if (newObj instanceof Photo) {
                    photoPublicQueryService.invalidateTagCache();
                }
            }

            @Override
            public void onDelete(Extension extension) {
                if (extension instanceof Photo) {
                    photoPublicQueryService.invalidateTagCache();
                }
            }

            @Override
            public void dispose() {
                disposed = true;
            }

            @Override
            public boolean isDisposed() {
                return disposed;
            }
        };
        client.watch(photoWatcher);
    }

    @Override
    public void stop() {
        if (photoWatcher != null) {
            photoWatcher.dispose();
        }
        schemeManager.unregister(Scheme.buildFromType(Photo.class));
        schemeManager.unregister(Scheme.buildFromType(PhotoGroup.class));
    }

    static String computeEffectiveTimeIndex(Photo photo) {
        Instant dt = photo.getExif() == null ? null : photo.getExif().getDateTimeOriginal();
        if (dt != null) {
            return dt.toString();
        }
        Instant created = photo.getMetadata() == null
            ? null : photo.getMetadata().getCreationTimestamp();
        return created == null ? "" : created.toString();
    }
}
