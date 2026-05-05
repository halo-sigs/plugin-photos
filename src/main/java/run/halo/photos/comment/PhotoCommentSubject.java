package run.halo.photos.comment;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import run.halo.app.content.comment.CommentSubject;
import run.halo.app.extension.GroupVersionKind;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.Ref;
import run.halo.app.infra.ExternalLinkProcessor;
import run.halo.photos.Photo;

/**
 * <p>Comment subject for photo.</p>
 * This class helps to get comment subject by name when comment list query.
 *
 * @author ryanwang
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
class PhotoCommentSubject implements CommentSubject<Photo> {

    private final ReactiveExtensionClient client;
    private final ExternalLinkProcessor externalLinkProcessor;
    private final GroupVersionKind gvk = GroupVersionKind.fromExtension(Photo.class);

    @Override
    public Mono<Photo> get(String name) {
        return client.fetch(Photo.class, name);
    }

    @Override
    public Mono<SubjectDisplay> getSubjectDisplay(String name) {
        return get(name).map(photo -> {
            var displayName = photo.getSpec() == null
                ? name : photo.getSpec().getDisplayName();
            var photoUrl = externalLinkProcessor.processLink("/photos/" + name);
            return new SubjectDisplay(displayName, photoUrl, "照片");
        });
    }

    @Override
    public boolean supports(Ref ref) {
        Assert.notNull(ref, "Subject ref must not be null.");
        return Objects.equals(gvk.group(), ref.getGroup())
            && Objects.equals(gvk.kind(), ref.getKind());
    }
}
