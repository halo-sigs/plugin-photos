package run.halo.photos;

import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static run.halo.app.extension.index.query.Queries.and;
import static run.halo.app.extension.index.query.Queries.contains;
import static run.halo.app.extension.index.query.Queries.empty;
import static run.halo.app.extension.index.query.Queries.equal;
import static run.halo.app.extension.router.selector.SelectorUtil.labelAndFieldSelectorToListOptions;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;
import run.halo.app.core.extension.endpoint.SortResolver;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.PageRequest;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.index.query.Condition;
import run.halo.app.extension.router.IListRequest;
import run.halo.app.extension.router.SortableRequest;
import run.halo.app.extension.router.selector.FieldSelector;

/**
 * A query object for public {@link Photo} list.
 */
public class PhotoPublicQuery extends SortableRequest {
    public static final String DATE_TIME_ORIGINAL_SORT = "spec.dateTimeOriginal";

    public PhotoPublicQuery(ServerWebExchange exchange) {
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

    public boolean isEffectiveTimeSort() {
        var sort = resolvedSort();
        if (sort.isUnsorted()) {
            return true;
        }
        return sort.stream()
            .findFirst()
            .map(order -> DATE_TIME_ORIGINAL_SORT.equals(order.getProperty()))
            .orElse(false);
    }

    public boolean isEffectiveTimeAscending() {
        return resolvedSort().stream()
            .filter(order -> DATE_TIME_ORIGINAL_SORT.equals(order.getProperty()))
            .findFirst()
            .map(Sort.Order::isAscending)
            .orElse(false);
    }

    /**
     * Returns the sort to apply. When no {@code sort} query parameter is provided the default
     * order is shooting time descending, falling back to creation timestamp then name.
     */
    @Override
    public Sort getSort() {
        var sort = resolvedSort();
        if (sort.isUnsorted()) {
            return Sort.by(
                Sort.Order.desc(DATE_TIME_ORIGINAL_SORT),
                Sort.Order.desc("metadata.creationTimestamp"),
                Sort.Order.asc("metadata.name")
            );
        }
        return sort.and(Sort.by(
            Sort.Order.desc("metadata.creationTimestamp"),
            Sort.Order.asc("metadata.name")
        ));
    }

    /**
     * Build {@link ListOptions} from query params.
     *
     * @return a list options.
     */
    public ListOptions toListOptions() {
        var listOptions =
            labelAndFieldSelectorToListOptions(getLabelSelector(), getFieldSelector());
        Condition query = empty();
        if (StringUtils.isNotBlank(getKeyword())) {
            query = and(query, contains("spec.displayName", getKeyword()));
        }
        if (isUngrouped()) {
            query = and(query, equal("spec.groupName", ""));
        } else if (StringUtils.isNotBlank(getGroup())) {
            query = and(query, equal("spec.groupName", getGroup()));
        }
        if (StringUtils.isNotBlank(getTag())) {
            query = and(query, equal("spec.tags", getTag()));
        }
        if (listOptions.getFieldSelector() != null) {
            query = and(query, (Condition) listOptions.getFieldSelector().query());
        }
        listOptions.setFieldSelector(FieldSelector.of(query));
        return listOptions;
    }

    public PageRequest toPageRequest() {
        return PageRequestImpl.of(getPage(), getSize(), getSort());
    }

    public static void buildParameters(Builder builder) {
        IListRequest.buildParameters(builder);
        builder.parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("sort")
                .description("Sort criteria: field,(asc|desc). "
                    + "Supported fields: spec.dateTimeOriginal, metadata.creationTimestamp. "
                    + "Sorting by spec.dateTimeOriginal uses metadata.creationTimestamp "
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
