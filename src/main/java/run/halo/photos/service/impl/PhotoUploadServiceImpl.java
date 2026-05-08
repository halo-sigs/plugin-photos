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
import java.util.List;
import java.util.HashMap;
import java.util.TimeZone;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.attachment.Attachment;
import run.halo.app.core.extension.service.AttachmentService;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.photos.Photo;
import run.halo.photos.service.PhotoUploadService;

/**
 * Implementation of {@link PhotoUploadService}.
 *
 * @author ryanwang
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PhotoUploadServiceImpl implements PhotoUploadService {

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    private final AttachmentService attachmentService;
    private final ReactiveExtensionClient client;
    private final ReactiveSettingFetcher settingFetcher;

    @Override
    public Mono<Photo> upload(FilePart filePart, String groupName) {
        var mediaType = resolveMediaType(filePart);
        if (!isImageMediaType(mediaType)) {
            return Mono.error(new ServerWebInputException("不支持的图片格式，仅支持 jpeg、png、webp、gif、heic 和 heif。"));
        }
        var contentLength = filePart.headers().getContentLength();
        if (contentLength > 0 && contentLength > MAX_FILE_SIZE) {
            return Mono.error(new ServerWebInputException("图片大小超过 50MB 限制。"));
        }
        return readFileContent(filePart)
            .flatMap(fileContent -> {
                var filename = filePart.filename();
                var exifData = extractExif(fileContent);
                var attachmentConfig = fetchAttachmentConfig();
                return attachmentConfig.flatMap(config -> {
                    if (StringUtils.isBlank(config.policyName())) {
                        return Mono.error(() ->
                            new ServerWebInputException("附件存储策略未配置，请先在插件设置中配置。"));
                    }
                    return uploadAttachment(config.policyName(), config.groupName(),
                        filename, fileContent, mediaType)
                        .flatMap(attachment -> createPhoto(attachment, groupName, filename, exifData));
                });
            });
    }

    private Mono<byte[]> readFileContent(FilePart filePart) {
        return DataBufferUtils.join(filePart.content())
            .map(dataBuffer -> {
                try {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    return bytes;
                } finally {
                    DataBufferUtils.release(dataBuffer);
                }
            })
            .flatMap(bytes -> {
                if (bytes.length > MAX_FILE_SIZE) {
                    return Mono.error(new ServerWebInputException("图片大小超过 50MB 限制。"));
                }
                return Mono.just(bytes);
            });
    }

    private Mono<Attachment> uploadAttachment(String policyName, String attachmentGroupName,
        String filename, byte[] content, MediaType mediaType) {
        var dataBufferFlux = DataBufferUtils.readInputStream(
            () -> new ByteArrayInputStream(content),
            new org.springframework.core.io.buffer.DefaultDataBufferFactory(),
            8192);
        return attachmentService.upload(policyName, attachmentGroupName, filename, dataBufferFlux,
            mediaType);
    }

    private static MediaType resolveMediaType(FilePart filePart) {
        var headerType = filePart.headers().getContentType();
        if (headerType != null) {
            return headerType;
        }
        return MediaTypeFactory.getMediaType(filePart.filename()).orElse(null);
    }

    private static boolean isImageMediaType(MediaType mediaType) {
        if (mediaType == null) {
            return false;
        }
        return mediaType.getType().equals("image")
            && List.of("jpeg", "jpg", "png", "webp", "gif", "heic", "heif")
                .contains(mediaType.getSubtype());
    }

    private Mono<Photo> createPhoto(Attachment attachment, String groupName, String filename,
        ExifData exifData) {
        var photo = new Photo();

        var metadata = new run.halo.app.extension.Metadata();
        metadata.setGenerateName("photo-");
        metadata.setAnnotations(new HashMap<>());
        photo.setMetadata(metadata);

        var spec = new Photo.PhotoSpec();
        spec.setDisplayName(filename);
        spec.setUrl(attachment.getStatus().getPermalink());
        spec.setGroupName(groupName);
        photo.setSpec(spec);

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

        // Only attach exif if at least one field was populated
        if (exifData.hasData()) {
            photo.setExif(exif);
        }

        return client.create(photo);
    }

    private Mono<AttachmentConfig> fetchAttachmentConfig() {
        return settingFetcher.getSettingValue("base")
            .map(setting -> {
                var policyName = setting.has("policyName")
                    ? setting.get("policyName").asText("") : "";
                var groupName = setting.has("groupName")
                    ? setting.get("groupName").asText("") : "";
                return new AttachmentConfig(policyName, groupName);
            })
            .switchIfEmpty(Mono.just(new AttachmentConfig("", "")));
    }

    private record AttachmentConfig(String policyName, String groupName) {
    }

    private ExifData extractExif(byte[] fileContent) {
        try {
            var metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(fileContent));
            return new ExifData(metadata);
        } catch (ImageProcessingException | IOException e) {
            log.warn("Failed to extract EXIF data: {}", e.getMessage());
            return new ExifData(null);
        }
    }

    private static class ExifData {
        private final Metadata metadata;

        ExifData(Metadata metadata) {
            this.metadata = metadata;
        }

        boolean hasData() {
            return metadata != null;
        }

        String getMake() {
            var dir = getDir(ExifIFD0Directory.class);
            return dir != null ? dir.getString(ExifIFD0Directory.TAG_MAKE) : null;
        }

        String getModel() {
            var dir = getDir(ExifIFD0Directory.class);
            return dir != null ? dir.getString(ExifIFD0Directory.TAG_MODEL) : null;
        }

        String getLensModel() {
            var dir = getDir(ExifSubIFDDirectory.class);
            return dir != null ? dir.getString(ExifSubIFDDirectory.TAG_LENS_MODEL) : null;
        }

        String getSoftware() {
            var dir = getDir(ExifIFD0Directory.class);
            return dir != null ? dir.getString(ExifIFD0Directory.TAG_SOFTWARE) : null;
        }

        Instant getDateTimeOriginal() {
            var dir = getDir(ExifSubIFDDirectory.class);
            if (dir == null) {
                return null;
            }
            var date = dir.getDateOriginal(TimeZone.getDefault());
            return date != null ? date.toInstant() : null;
        }

        Double getFNumber() {
            var dir = getDir(ExifSubIFDDirectory.class);
            return dir != null ? dir.getDoubleObject(ExifSubIFDDirectory.TAG_FNUMBER) : null;
        }

        String getExposureTime() {
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
            // Simplify rationals like 10/1250 → 1/125
            long gcd = gcd(rational.getNumerator(), rational.getDenominator());
            return (rational.getNumerator() / gcd) + "/" + (rational.getDenominator() / gcd);
        }

        Integer getIso() {
            var dir = getDir(ExifSubIFDDirectory.class);
            return dir != null ? dir.getInteger(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT) : null;
        }

        Double getFocalLength() {
            var dir = getDir(ExifSubIFDDirectory.class);
            return dir != null ? dir.getDoubleObject(ExifSubIFDDirectory.TAG_FOCAL_LENGTH) : null;
        }

        Integer getFocalLengthIn35mm() {
            var dir = getDir(ExifSubIFDDirectory.class);
            return dir != null
                ? dir.getInteger(ExifSubIFDDirectory.TAG_35MM_FILM_EQUIV_FOCAL_LENGTH) : null;
        }

        Integer getFlash() {
            var dir = getDir(ExifSubIFDDirectory.class);
            return dir != null ? dir.getInteger(ExifSubIFDDirectory.TAG_FLASH) : null;
        }

        Integer getWhiteBalance() {
            var dir = getDir(ExifSubIFDDirectory.class);
            return dir != null ? dir.getInteger(ExifSubIFDDirectory.TAG_WHITE_BALANCE) : null;
        }

        Integer getExposureMode() {
            var dir = getDir(ExifSubIFDDirectory.class);
            return dir != null ? dir.getInteger(ExifSubIFDDirectory.TAG_EXPOSURE_MODE) : null;
        }

        Integer getExposureProgram() {
            var dir = getDir(ExifSubIFDDirectory.class);
            return dir != null ? dir.getInteger(ExifSubIFDDirectory.TAG_EXPOSURE_PROGRAM) : null;
        }

        Integer getMeteringMode() {
            var dir = getDir(ExifSubIFDDirectory.class);
            return dir != null ? dir.getInteger(ExifSubIFDDirectory.TAG_METERING_MODE) : null;
        }

        Integer getImageWidth() {
            var dir = getDir(ExifSubIFDDirectory.class);
            return dir != null ? dir.getInteger(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH) : null;
        }

        Integer getImageHeight() {
            var dir = getDir(ExifSubIFDDirectory.class);
            return dir != null ? dir.getInteger(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT) : null;
        }

        Double getGpsLatitude() {
            var gps = getGpsLocation();
            return gps != null ? gps.getLatitude() : null;
        }

        Double getGpsLongitude() {
            var gps = getGpsLocation();
            return gps != null ? gps.getLongitude() : null;
        }

        Double getGpsAltitude() {
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
