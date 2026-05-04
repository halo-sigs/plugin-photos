package run.halo.photos;

import static run.halo.app.extension.index.query.Queries.and;
import static run.halo.app.extension.index.query.Queries.contains;
import static run.halo.app.extension.index.query.Queries.empty;
import static run.halo.app.extension.index.query.Queries.equal;
import static run.halo.app.extension.router.selector.SelectorUtil.labelAndFieldSelectorToListOptions;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.PageRequest;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.index.query.Condition;
import run.halo.app.extension.router.selector.FieldSelector;

/**
 * A query object for public {@link Photo} list.
 */
public class PhotoPublicQuery extends PhotoQuery {

    public PhotoPublicQuery(ServerWebExchange exchange) {
        super(exchange);
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
}
