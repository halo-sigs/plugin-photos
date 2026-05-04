package run.halo.photos;

import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;
import run.halo.app.core.extension.endpoint.SortResolver;
import run.halo.app.extension.router.IListRequest;
import run.halo.app.extension.router.SortableRequest;

/**
 * A query object for {@link Photo} list.
 *
 * @author LIlGG
 * @since 1.0.0
 */
public class PhotoQuery extends SortableRequest {
    static final String DATE_TIME_ORIGINAL_SORT = "exif.dateTimeOriginal";
    static final String EFFECTIVE_TIME_INDEX = "effectiveTime";

    public PhotoQuery(ServerWebExchange exchange) {
        super(exchange);
    }

    public String getGroup() {
        return queryParams.getFirst("group");
    }

    public boolean isUngrouped() {
        return Boolean.parseBoolean(queryParams.getFirst("ungrouped"));
    }

    @Nullable
    public String getKeyword() {
        return queryParams.getFirst("keyword");
    }

    public String getTag() {
        return queryParams.getFirst("tag");
    }

    /**
     * Returns the sort to apply. When no {@code sort} query parameter is provided the default
     * order is shooting time descending, falling back to creation timestamp then name.
     *
     * <p>The public sort field name {@code exif.dateTimeOriginal} is translated to the internal
     * {@code effectiveTime} index, which stores the EXIF time when present and the creation
     * timestamp otherwise. This lets the database serve effective-time sort directly.
     */
    @Override
    public Sort getSort() {
        var sort = resolvedSort();
        if (sort.isUnsorted()) {
            return Sort.by(
                Sort.Order.desc(EFFECTIVE_TIME_INDEX),
                Sort.Order.desc("metadata.creationTimestamp"),
                Sort.Order.asc("metadata.name")
            );
        }
        var translated = Sort.by(sort.stream()
            .map(order -> DATE_TIME_ORIGINAL_SORT.equals(order.getProperty())
                ? new Sort.Order(order.getDirection(), EFFECTIVE_TIME_INDEX)
                : order)
            .toList());
        return translated.and(Sort.by(
            Sort.Order.desc("metadata.creationTimestamp"),
            Sort.Order.asc("metadata.name")
        ));
    }

    public static void buildParameters(Builder builder) {
        IListRequest.buildParameters(builder);
        builder.parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("sort")
                .description("Sort criteria: field,(asc|desc). "
                    + "Supported fields: exif.dateTimeOriginal, metadata.creationTimestamp. "
                    + "Sorting by exif.dateTimeOriginal uses metadata.creationTimestamp "
                    + "when EXIF time is missing.")
                .implementation(String.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("keyword")
                .description("Photos filtered by keyword.")
                .implementation(String.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("group")
                .description("photo group name")
                .implementation(String.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("ungrouped")
                .description("If true, return only photos without a group assigned. "
                    + "Overrides the 'group' parameter when set.")
                .implementation(Boolean.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("tag")
                .description("photo tag")
                .implementation(String.class)
                .required(false));
    }

    private Sort resolvedSort() {
        return SortResolver.defaultInstance.resolve(exchange);
    }
}
