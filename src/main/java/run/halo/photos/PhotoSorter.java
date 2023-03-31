package run.halo.photos;

import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.util.comparator.Comparators;

/**
 * A sorter for {@link Photo}.
 *
 * @author LIlGG
 * @since 1.0.0
 */
public enum PhotoSorter {
    DISPLAY_NAME,
    
    CREATE_TIME;
    
    static final Function<Photo, String> name = photo -> photo.getMetadata()
        .getName();
    
    /**
     * Converts {@link Comparator} from {@link PhotoSorter} and ascending.
     *
     * @param sorter    a {@link PhotoSorter}
     * @param ascending ascending if true, otherwise descending
     * @return a {@link Comparator} of {@link Photo}
     */
    public static Comparator<Photo> from(PhotoSorter sorter,
        Boolean ascending) {
        if (Objects.equals(true, ascending)) {
            return from(sorter);
        }
        return from(sorter).reversed();
    }
    
    /**
     * Converts {@link Comparator} from {@link PhotoSorter}.
     *
     * @param sorter a {@link PhotoSorter}
     * @return a {@link Comparator} of {@link Photo}
     */
    static Comparator<Photo> from(PhotoSorter sorter) {
        if (sorter == null) {
            return createTimeComparator();
        }
        if (CREATE_TIME.equals(sorter)) {
            Function<Photo, Instant> comparatorFunc
                = photo -> photo.getMetadata().getCreationTimestamp();
            return Comparator.comparing(comparatorFunc).thenComparing(name);
        }
        
        if (DISPLAY_NAME.equals(sorter)) {
            Function<Photo, String> comparatorFunc = moment -> moment.getSpec()
                .getDisplayName();
            return Comparator.comparing(comparatorFunc, Comparators.nullsLow())
                .thenComparing(name);
        }
        
        throw new IllegalStateException("Unsupported sort value: " + sorter);
    }
    
    /**
     * Converts {@link PhotoSorter} from string.
     *
     * @param sort sort string
     * @return a {@link PhotoSorter}
     */
    static PhotoSorter convertFrom(String sort) {
        for (PhotoSorter sorter : values()) {
            if (sorter.name().equalsIgnoreCase(sort)) {
                return sorter;
            }
        }
        return null;
    }
    
    /**
     * Creates a {@link Comparator} of {@link Photo} by creation time.
     *
     * @return a {@link Comparator} of {@link Photo}
     */
    static Comparator<Photo> createTimeComparator() {
        Function<Photo, Instant> comparatorFunc = photo -> photo.getMetadata()
            .getCreationTimestamp();
        return Comparator.comparing(comparatorFunc).thenComparing(name);
    }
}
