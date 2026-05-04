package run.halo.photos;

import java.time.Instant;
import java.util.Comparator;
import run.halo.app.extension.MetadataOperator;

/**
 * Shared photo sorting helpers.
 */
public final class PhotoSortUtils {

    private PhotoSortUtils() {
    }

    public static Comparator<Photo> effectiveTimeComparator(boolean ascending) {
        return Comparator
            .comparing(PhotoSortUtils::effectiveTime, instantComparator(ascending))
            .thenComparing(PhotoSortUtils::creationTimestamp, instantComparator(false))
            .thenComparing(PhotoSortUtils::metadataName,
                Comparator.nullsLast(Comparator.naturalOrder()));
    }

    static Instant effectiveTime(Photo photo) {
        var dateTimeOriginal = dateTimeOriginal(photo);
        return dateTimeOriginal == null ? creationTimestamp(photo) : dateTimeOriginal;
    }

    static Instant dateTimeOriginal(Photo photo) {
        var exif = photo == null ? null : photo.getExif();
        return exif == null ? null : exif.getDateTimeOriginal();
    }

    static Instant creationTimestamp(Photo photo) {
        MetadataOperator metadata = photo == null ? null : photo.getMetadata();
        return metadata == null ? null : metadata.getCreationTimestamp();
    }

    static String metadataName(Photo photo) {
        MetadataOperator metadata = photo == null ? null : photo.getMetadata();
        return metadata == null ? null : metadata.getName();
    }

    private static Comparator<Instant> instantComparator(boolean ascending) {
        Comparator<Instant> comparator = ascending
            ? Comparator.naturalOrder()
            : Comparator.reverseOrder();
        return Comparator.nullsLast(comparator);
    }

    public static Comparator<PhotoGroup> groupComparator() {
        return (g1, g2) -> {
            var p1 = g1.getSpec() != null && g1.getSpec().getPriority() != null
                ? g1.getSpec().getPriority() : 0;
            var p2 = g2.getSpec() != null && g2.getSpec().getPriority() != null
                ? g2.getSpec().getPriority() : 0;
            int priorityCompare = Integer.compare(p2, p1);
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            var t1 = g1.getMetadata() != null ? g1.getMetadata().getCreationTimestamp() : null;
            var t2 = g2.getMetadata() != null ? g2.getMetadata().getCreationTimestamp() : null;
            if (t1 == null && t2 == null) {
                return 0;
            }
            if (t1 == null) {
                return 1;
            }
            if (t2 == null) {
                return -1;
            }
            int timeCompare = t2.compareTo(t1);
            if (timeCompare != 0) {
                return timeCompare;
            }
            var n1 = g1.getMetadata() != null ? g1.getMetadata().getName() : "";
            var n2 = g2.getMetadata() != null ? g2.getMetadata().getName() : "";
            return n1.compareTo(n2);
        };
    }
}
