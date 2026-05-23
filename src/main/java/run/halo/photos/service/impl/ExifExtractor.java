package run.halo.photos.service.impl;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.TimeZone;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import run.halo.photos.Photo;

/**
 * Extracts EXIF metadata from image byte arrays using
 * {@code com.drew.metadata} (metadata-extractor).
 */
@Slf4j
@Component
public class ExifExtractor {

    /**
     * Extract EXIF data from image bytes.
     *
     * @param fileContent image file bytes
     * @return extracted EXIF data, or empty data if parsing fails
     */
    public ExifData extractExif(byte[] fileContent) {
        try {
            var metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(fileContent));
            return new ExifData(metadata);
        } catch (ImageProcessingException | IOException e) {
            log.warn("Failed to extract EXIF data: {}", e.getMessage());
            return new ExifData(null);
        }
    }

    /**
     * Populate a {@link Photo.PhotoExif} from extracted EXIF data.
     *
     * @param exifData extracted data
     * @return populated exif object, or null if no data was extracted
     */
    public Photo.PhotoExif toPhotoExif(ExifData exifData) {
        if (!exifData.hasData()) {
            return null;
        }
        var exif = new Photo.PhotoExif();
        exif.setMake(exifData.getMake());
        exif.setModel(exifData.getModel());
        exif.setLensModel(exifData.getLensModel());
        exif.setSoftware(exifData.getSoftware());
        exif.setDateTimeOriginal(exifData.getDateTimeOriginal());
        exif.setFNumber(exifData.getFNumber());
        exif.setExposureTime(exifData.getExposureTime());
        exif.setIso(exifData.getIso());
        exif.setFocalLength(exifData.getFocalLength());
        exif.setFocalLengthIn35mm(exifData.getFocalLengthIn35mm());
        exif.setFlash(exifData.getFlash());
        exif.setWhiteBalance(exifData.getWhiteBalance());
        exif.setExposureMode(exifData.getExposureMode());
        exif.setExposureProgram(exifData.getExposureProgram());
        exif.setMeteringMode(exifData.getMeteringMode());
        exif.setImageWidth(exifData.getImageWidth());
        exif.setImageHeight(exifData.getImageHeight());
        exif.setGpsLatitude(exifData.getGpsLatitude());
        exif.setGpsLongitude(exifData.getGpsLongitude());
        exif.setGpsAltitude(exifData.getGpsAltitude());
        return exif;
    }

    /**
     * Extracted EXIF data wrapper backed by {@link Metadata}.
     */
    public static class ExifData {
        private final Metadata metadata;

        ExifData(Metadata metadata) {
            this.metadata = metadata;
        }

        /**
         * Returns whether any EXIF data was successfully parsed.
         *
         * @return true when metadata is non-null
         */
        public boolean hasData() {
            return metadata != null;
        }

        /**
         * Camera manufacturer, e.g. "Canon".
         *
         * @return make string, or null
         */
        public String getMake() {
            var dir = getDir(ExifIFD0Directory.class);
            return dir != null ? dir.getString(ExifIFD0Directory.TAG_MAKE) : null;
        }

        /**
         * Camera model name.
         *
         * @return model string, or null
         */
        public String getModel() {
            var dir = getDir(ExifIFD0Directory.class);
            return dir != null ? dir.getString(ExifIFD0Directory.TAG_MODEL) : null;
        }

        /**
         * Lens model name.
         *
         * @return lens model string, or null
         */
        public String getLensModel() {
            var dir = getDir(ExifSubIFDDirectory.class);
            return dir != null ? dir.getString(ExifSubIFDDirectory.TAG_LENS_MODEL) : null;
        }

        /**
         * Software used to process the image.
         *
         * @return software string, or null
         */
        public String getSoftware() {
            var dir = getDir(ExifIFD0Directory.class);
            return dir != null ? dir.getString(ExifIFD0Directory.TAG_SOFTWARE) : null;
        }

        /**
         * Original capture time from EXIF DateTimeOriginal.
         *
         * @return instant, or null
         */
        public Instant getDateTimeOriginal() {
            var dir = getDir(ExifSubIFDDirectory.class);
            if (dir == null) {
                return null;
            }
            var date = dir.getDateOriginal(TimeZone.getDefault());
            return date != null ? date.toInstant() : null;
        }

        /**
         * Aperture f-number.
         *
         * @return f-number, or null
         */
        public Double getFNumber() {
            var dir = getDir(ExifSubIFDDirectory.class);
            return dir != null ? dir.getDoubleObject(ExifSubIFDDirectory.TAG_FNUMBER) : null;
        }

        /**
         * Exposure time as a fraction string, e.g. "1/125".
         *
         * @return exposure time string, or null
         */
        public String getExposureTime() {
            var dir = getDir(ExifSubIFDDirectory.class);
            if (dir == null) {
                return null;
            }
            var rational = dir.getRational(ExifSubIFDDirectory.TAG_EXPOSURE_TIME);
            if (rational == null) {
                return null;
            }
            if (rational.getNumerator() == 1 || rational.getNumerator() == 0) {
                return rational.getNumerator() + "/" + rational.getDenominator();
            }
            // Simplify rationals like 10/1250 -> 1/125
            long gcd = gcd(rational.getNumerator(), rational.getDenominator());
            return (rational.getNumerator() / gcd) + "/" + (rational.getDenominator() / gcd);
        }

        /**
         * ISO sensitivity value.
         *
         * @return ISO value, or null
         */
        public Integer getIso() {
            var dir = getDir(ExifSubIFDDirectory.class);
            return dir != null ? dir.getInteger(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT) : null;
        }

        /**
         * Focal length in millimetres.
         *
         * @return focal length, or null
         */
        public Double getFocalLength() {
            var dir = getDir(ExifSubIFDDirectory.class);
            return dir != null ? dir.getDoubleObject(ExifSubIFDDirectory.TAG_FOCAL_LENGTH) : null;
        }

        /**
         * Focal length in 35 mm equivalent.
         *
         * @return 35 mm equivalent focal length, or null
         */
        public Integer getFocalLengthIn35mm() {
            var dir = getDir(ExifSubIFDDirectory.class);
            return dir != null
                ? dir.getInteger(ExifSubIFDDirectory.TAG_35MM_FILM_EQUIV_FOCAL_LENGTH) : null;
        }

        /**
         * Flash firing status.
         *
         * @return flash value, or null
         */
        public Integer getFlash() {
            var dir = getDir(ExifSubIFDDirectory.class);
            return dir != null ? dir.getInteger(ExifSubIFDDirectory.TAG_FLASH) : null;
        }

        /**
         * White balance mode.
         *
         * @return white balance value, or null
         */
        public Integer getWhiteBalance() {
            var dir = getDir(ExifSubIFDDirectory.class);
            return dir != null ? dir.getInteger(ExifSubIFDDirectory.TAG_WHITE_BALANCE) : null;
        }

        /**
         * Exposure mode.
         *
         * @return exposure mode value, or null
         */
        public Integer getExposureMode() {
            var dir = getDir(ExifSubIFDDirectory.class);
            return dir != null ? dir.getInteger(ExifSubIFDDirectory.TAG_EXPOSURE_MODE) : null;
        }

        /**
         * Exposure program.
         *
         * @return exposure program value, or null
         */
        public Integer getExposureProgram() {
            var dir = getDir(ExifSubIFDDirectory.class);
            return dir != null ? dir.getInteger(ExifSubIFDDirectory.TAG_EXPOSURE_PROGRAM) : null;
        }

        /**
         * Metering mode.
         *
         * @return metering mode value, or null
         */
        public Integer getMeteringMode() {
            var dir = getDir(ExifSubIFDDirectory.class);
            return dir != null ? dir.getInteger(ExifSubIFDDirectory.TAG_METERING_MODE) : null;
        }

        /**
         * Image width in pixels.
         *
         * @return width, or null
         */
        public Integer getImageWidth() {
            var dir = getDir(ExifSubIFDDirectory.class);
            return dir != null ? dir.getInteger(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH) : null;
        }

        /**
         * Image height in pixels.
         *
         * @return height, or null
         */
        public Integer getImageHeight() {
            var dir = getDir(ExifSubIFDDirectory.class);
            return dir != null ? dir.getInteger(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT) : null;
        }

        /**
         * GPS latitude in decimal degrees.
         *
         * @return latitude, or null
         */
        public Double getGpsLatitude() {
            var gps = getGpsLocation();
            return gps != null ? gps.getLatitude() : null;
        }

        /**
         * GPS longitude in decimal degrees.
         *
         * @return longitude, or null
         */
        public Double getGpsLongitude() {
            var gps = getGpsLocation();
            return gps != null ? gps.getLongitude() : null;
        }

        /**
         * GPS altitude in metres.
         *
         * @return altitude, or null
         */
        public Double getGpsAltitude() {
            var dir = getDir(GpsDirectory.class);
            return dir != null ? dir.getDoubleObject(GpsDirectory.TAG_ALTITUDE) : null;
        }

        private GeoLocation getGpsLocation() {
            var dir = getDir(GpsDirectory.class);
            return dir != null ? dir.getGeoLocation() : null;
        }

        private <T extends com.drew.metadata.Directory> T getDir(Class<T> type) {
            return metadata != null ? metadata.getFirstDirectoryOfType(type) : null;
        }

        private static long gcd(long a, long b) {
            a = Math.abs(a);
            b = Math.abs(b);
            if (a == 0 && b == 0) {
                return 1;
            }
            return b == 0 ? a : gcd(b, a % b);
        }
    }
}
