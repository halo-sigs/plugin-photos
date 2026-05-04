package run.halo.photos.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import tools.jackson.databind.node.JsonNodeFactory;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.attachment.Attachment;
import run.halo.app.core.extension.service.AttachmentService;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.photos.Photo;

class PhotoUploadServiceImplTest {

    private final AttachmentService attachmentService = mock(AttachmentService.class);
    private final ReactiveExtensionClient client = mock(ReactiveExtensionClient.class);
    private final ReactiveSettingFetcher settingFetcher = mock(ReactiveSettingFetcher.class);

    private PhotoUploadServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PhotoUploadServiceImpl(attachmentService, client, settingFetcher);
    }

    @Test
    void uploadWithUnsupportedMediaTypeShouldFail() {
        var filePart = filePartHeadersOnly("video.mp4", MediaType.parseMediaType("video/mp4"), -1);

        assertThatThrownBy(() -> service.upload(filePart, null).block())
            .isInstanceOf(ServerWebInputException.class)
            .hasMessageContaining("不支持的图片格式");
    }

    @Test
    void uploadWithNullMediaTypeAndUnrecognizedExtensionShouldFail() {
        // No Content-Type header and extension ".bin" is not recognized as an image
        var filePart = filePartHeadersOnly("unknown.bin", null, -1);

        assertThatThrownBy(() -> service.upload(filePart, null).block())
            .isInstanceOf(ServerWebInputException.class)
            .hasMessageContaining("不支持的图片格式");
    }

    @Test
    void uploadFileTooLargeShouldFail() {
        long overLimit = 51L * 1024 * 1024; // 51 MB > 50 MB limit
        var filePart = filePartHeadersOnly("photo.jpg", MediaType.IMAGE_JPEG, overLimit);

        assertThatThrownBy(() -> service.upload(filePart, null).block())
            .isInstanceOf(ServerWebInputException.class)
            .hasMessageContaining("50MB");
    }

    @Test
    void uploadWithMissingPolicyNameShouldFail() {
        var filePart = filePartWithContent("photo.jpg", MediaType.IMAGE_JPEG, new byte[100]);
        var setting = JsonNodeFactory.instance.objectNode();
        setting.put("policyName", "");
        when(settingFetcher.getSettingValue("base")).thenReturn(Mono.just(setting));

        assertThatThrownBy(() -> service.upload(filePart, null).block())
            .isInstanceOf(ServerWebInputException.class)
            .hasMessageContaining("附件存储策略未配置");
    }

    @Test
    void uploadWithNoSettingShouldFail() {
        // switchIfEmpty provides AttachmentConfig("", "") which triggers the error
        var filePart = filePartWithContent("photo.jpg", MediaType.IMAGE_JPEG, new byte[100]);
        when(settingFetcher.getSettingValue("base")).thenReturn(Mono.empty());

        assertThatThrownBy(() -> service.upload(filePart, null).block())
            .isInstanceOf(ServerWebInputException.class)
            .hasMessageContaining("附件存储策略未配置");
    }

    @Test
    void uploadSuccessfullyCreatesPhotoWithGroupName() {
        var filePart = filePartWithContent("photo.jpg", MediaType.IMAGE_JPEG, new byte[100]);

        var setting = JsonNodeFactory.instance.objectNode();
        setting.put("policyName", "local");
        setting.put("groupName", "attachments");
        when(settingFetcher.getSettingValue("base")).thenReturn(Mono.just(setting));

        var attachment = attachment("photo.jpg", "/attachments/photo.jpg");
        when(attachmentService.upload(anyString(), anyString(), anyString(), any(), any(MediaType.class)))
            .thenReturn(Mono.just(attachment));
        when(client.create(any(Photo.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        var result = service.upload(filePart, "trips").block();

        assertThat(result).isNotNull();
        assertThat(result.getSpec().getGroupName()).isEqualTo("trips");
        assertThat(result.getSpec().getDisplayName()).isEqualTo("photo.jpg");
        assertThat(result.getSpec().getUrl()).isEqualTo("/attachments/photo.jpg");
    }

    @Test
    void uploadSuccessfullyCreatesPhotoWithNullGroupName() {
        var filePart = filePartWithContent("photo.jpg", MediaType.IMAGE_JPEG, new byte[100]);

        var setting = JsonNodeFactory.instance.objectNode();
        setting.put("policyName", "local");
        when(settingFetcher.getSettingValue("base")).thenReturn(Mono.just(setting));

        var attachment = attachment("photo.jpg", "/attachments/photo.jpg");
        when(attachmentService.upload(anyString(), anyString(), anyString(), any(), any(MediaType.class)))
            .thenReturn(Mono.just(attachment));
        when(client.create(any(Photo.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        var result = service.upload(filePart, null).block();

        assertThat(result).isNotNull();
        assertThat(result.getSpec().getGroupName()).isNull();
    }

    @Test
    void uploadWebpImageShouldSucceed() {
        var filePart = filePartWithContent("photo.webp",
            MediaType.parseMediaType("image/webp"), new byte[100]);

        var setting = JsonNodeFactory.instance.objectNode();
        setting.put("policyName", "local");
        when(settingFetcher.getSettingValue("base")).thenReturn(Mono.just(setting));

        var attachment = attachment("photo.webp", "/attachments/photo.webp");
        when(attachmentService.upload(anyString(), anyString(), anyString(), any(), any(MediaType.class)))
            .thenReturn(Mono.just(attachment));
        when(client.create(any(Photo.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        var result = service.upload(filePart, null).block();

        assertThat(result).isNotNull();
        assertThat(result.getSpec().getDisplayName()).isEqualTo("photo.webp");
    }

    @Test
    void uploadFileSizeExactlyAtLimitShouldNotFail() {
        // Content-Length == MAX_FILE_SIZE (50 MB) is allowed (condition is >)
        long exactLimit = 50L * 1024 * 1024;
        var filePart = filePartHeadersOnly("photo.jpg", MediaType.IMAGE_JPEG, exactLimit);

        // Since the service proceeds to readFileContent, which calls filePart.content(),
        // and we haven't stubbed content(), it will return null. But the size check passes.
        // We assert the error is NOT about file size.
        assertThatThrownBy(() -> service.upload(filePart, null).block())
            .isNotInstanceOf(ServerWebInputException.class)
            .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain("50MB"));
    }

    private static FilePart filePartHeadersOnly(String filename, MediaType contentType,
        long contentLength) {
        var filePart = mock(FilePart.class);
        var headers = new HttpHeaders();
        if (contentType != null) {
            headers.setContentType(contentType);
        }
        if (contentLength >= 0) {
            headers.setContentLength(contentLength);
        }
        when(filePart.filename()).thenReturn(filename);
        when(filePart.headers()).thenReturn(headers);
        return filePart;
    }

    private static FilePart filePartWithContent(String filename, MediaType contentType,
        byte[] content) {
        var filePart = mock(FilePart.class);
        var headers = new HttpHeaders();
        headers.setContentType(contentType);
        headers.setContentLength(content.length);
        when(filePart.filename()).thenReturn(filename);
        when(filePart.headers()).thenReturn(headers);

        var factory = new DefaultDataBufferFactory();
        var buffer = factory.wrap(content);
        when(filePart.content()).thenReturn(Flux.just(buffer));
        return filePart;
    }

    private static Attachment attachment(String name, String permalink) {
        var metadata = new Metadata();
        metadata.setName(name);
        metadata.setCreationTimestamp(Instant.now());

        var status = mock(Attachment.AttachmentStatus.class);
        when(status.getPermalink()).thenReturn(permalink);

        var attachment = mock(Attachment.class);
        when(attachment.getMetadata()).thenReturn(metadata);
        when(attachment.getStatus()).thenReturn(status);
        return attachment;
    }
}
