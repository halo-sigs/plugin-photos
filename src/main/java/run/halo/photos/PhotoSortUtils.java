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
}
