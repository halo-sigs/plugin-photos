package run.halo.photos.service.impl;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
    private final ExifExtractor exifExtractor;

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
                var exifData = exifExtractor.extractExif(fileContent);
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
        ExifExtractor.ExifData exifData) {
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

        var exif = exifExtractor.toPhotoExif(exifData);
        if (exif != null) {
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
}
