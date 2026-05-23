package run.halo.photos;

/**
 * Static variable keys for view model used by {@link PhotoRouter}.
 *
 * @author guqing
 * @since 2.0.0
 */
public final class ModelConst {

    private ModelConst() {
    }

    /**
     * Thymeleaf template ID key placed in the model map.
     */
    public static final String TEMPLATE_ID = "_templateId";

    /**
     * Default page size for photo list pagination.
     */
    public static final Integer DEFAULT_PAGE_SIZE = 10;
}
