package run.halo.photos;

import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static run.halo.app.extension.router.QueryParamBuildUtil.sortParameter;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import run.halo.app.extension.router.IListRequest;

/**
 * A query object for {@link Photo} list.
 *
 * @author LIlGG
 * @since 1.0.0
 */
public class PhotoQuery extends IListRequest.QueryListRequest {
    
    public PhotoQuery(MultiValueMap<String, String> queryParams) {
        super(queryParams);
    }
    
    @Schema(description = "Photos filtered by group.")
    public String getGroup() {
        return StringUtils.defaultIfBlank(queryParams.getFirst("group"), null);
    }
    
    @Nullable
    @Schema(description = "Photos filtered by keyword.")
    public String getKeyword() {
        return StringUtils.defaultIfBlank(queryParams.getFirst("keyword"),
            null
        );
    }
    
    @Schema(description = "Photo collation.")
    public PhotoSorter getSort() {
        String sort = queryParams.getFirst("sort");
        return PhotoSorter.convertFrom(sort);
    }
    
    @Schema(description = "ascending order If it is true; otherwise, it is in descending order.")
    public Boolean getSortOrder() {
        String sortOrder = queryParams.getFirst("sortOrder");
        return convertBooleanOrNull(sortOrder);
    }
    
    private Boolean convertBooleanOrNull(String value) {
        return StringUtils.isBlank(value) ? null : Boolean.parseBoolean(value);
    }

    public static void buildParameters(Builder builder) {
        IListRequest.buildParameters(builder);
        builder.parameter(sortParameter())
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
        ;
    }
}
