package run.halo.photos.service.impl;

import static run.halo.app.extension.router.selector.SelectorUtil.labelAndFieldSelectorToPredicate;

import java.util.Comparator;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.extension.Extension;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.photos.Photo;
import run.halo.photos.PhotoQuery;
import run.halo.photos.PhotoSorter;
import run.halo.photos.service.PhotoService;

/**
 * Service implementation for {@link Photo}.
 *
 * @author LIlGG
 * @since 1.0.0
 */
@Component
public class PhotoServiceImpl implements PhotoService {
    
    private final ReactiveExtensionClient client;
    
    public PhotoServiceImpl(ReactiveExtensionClient client) {
        this.client = client;
    }
    
    @Override
    public Mono<ListResult<Photo>> listPhoto(PhotoQuery query) {
        Comparator<Photo> comparator = PhotoSorter.from(query.getSort(),
            query.getSortOrder()
        );
        return this.client.list(Photo.class, photoListPredicate(query),
            comparator, query.getPage(), query.getSize()
        );
    }
    
    Predicate<Photo> photoListPredicate(PhotoQuery query) {
        Predicate<Photo> predicate = photo -> true;
        String keyword = query.getKeyword();
        
        if (keyword != null) {
            predicate = predicate.and(photo -> {
                String displayName = photo.getSpec().getDisplayName();
                return StringUtils.containsIgnoreCase(displayName, keyword);
            });
        }
        
        String groupName = query.getGroup();
        if (groupName != null) {
            predicate = predicate.and(photo -> {
                String group = photo.getSpec().getGroupName();
                return StringUtils.equals(group, groupName);
            });
        }
        
        Predicate<Extension> labelAndFieldSelectorPredicate
            = labelAndFieldSelectorToPredicate(query.getLabelSelector(),
            query.getFieldSelector()
        );
        return predicate.and(labelAndFieldSelectorPredicate);
    }
}
