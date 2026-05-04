package run.halo.photos;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.util.UriComponentsBuilder;
import run.halo.photos.vo.PhotoVo;

/**
 * Per-request URL helper exposed to theme templates as <code>photoUrl</code>. Lets templates
 * build context-preserving links to the photo list and detail pages without hand-concatenating
 * query strings.
 */
public class PhotoUrlBuilder {

    static final String GROUP_PARAM = "group";
    static final String PAGE_PARAM = "page";
    static final String SIZE_PARAM = "size";

    private static final List<String> CONTEXT_PARAMS =
        List.of(GROUP_PARAM, PAGE_PARAM, SIZE_PARAM);

    private final Map<String, String> contextParams;

    public PhotoUrlBuilder(ServerRequest request) {
        this(extractContext(request));
    }

    PhotoUrlBuilder(Map<String, String> contextParams) {
        var filtered = new LinkedHashMap<String, String>();
        for (String param : CONTEXT_PARAMS) {
            String value = contextParams.get(param);
            if (StringUtils.isNotBlank(value)) {
                filtered.put(param, value);
            }
        }
        this.contextParams = filtered;
    }

    public String detail(PhotoVo photo) {
        return detail(photo, Map.of());
    }

    public String detail(PhotoVo photo, Map<String, ?> overrides) {
        Map<String, Object> params = new LinkedHashMap<>();
        for (String param : CONTEXT_PARAMS) {
            if (contextParams.containsKey(param)) {
                params.put(param, contextParams.get(param));
            }
            if (overrides != null && overrides.containsKey(param)) {
                Object override = overrides.get(param);
                if (isBlank(override)) {
                    params.remove(param);
                } else {
                    params.put(param, override);
                }
            }
        }
        var builder = UriComponentsBuilder.fromPath("/photos/" + photo.getMetadata().getName());
        params.forEach(builder::queryParam);
        return builder.build().toUriString();
    }

    public String list() {
        return "/photos";
    }

    public String list(String group) {
        var builder = UriComponentsBuilder.fromPath("/photos");
        if (StringUtils.isNotBlank(group)) {
            builder.queryParam(GROUP_PARAM, group);
        }
        return builder.build().toUriString();
    }

    public String list(String group, int page, int size) {
        var builder = UriComponentsBuilder.fromPath("/photos");
        if (StringUtils.isNotBlank(group)) {
            builder.queryParam(GROUP_PARAM, group);
        }
        if (page > 0) {
            builder.queryParam(PAGE_PARAM, page);
        }
        if (size > 0) {
            builder.queryParam(SIZE_PARAM, size);
        }
        return builder.build().toUriString();
    }

    private static Map<String, String> extractContext(ServerRequest request) {
        var snapshot = new LinkedHashMap<String, String>();
        for (String param : CONTEXT_PARAMS) {
            request.queryParam(param)
                .filter(StringUtils::isNotBlank)
                .ifPresent(value -> snapshot.put(param, value));
        }
        return snapshot;
    }

    private static boolean isBlank(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof CharSequence cs) {
            return StringUtils.isBlank(cs);
        }
        return false;
    }
}
