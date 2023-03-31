package run.halo.photos.finders;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;
import run.halo.photos.vo.PhotoGroupVo;
import run.halo.photos.vo.PhotoVo;


/**
 * A finder for {@link run.halo.photos.Photo}.
 *
 * @author LIlGG
 */
public interface PhotoFinder {
    
    /**
     * List all photos.
     *
     * @return a flux of photo vo
     */
    Flux<PhotoVo> listAll();
    
    /**
     * List photos by page.
     *
     * @param page page number
     * @param size page size
     * @return a mono of list result
     */
    Mono<ListResult<PhotoVo>> list(Integer page, Integer size);
    
    /**
     * List photos by page and group.
     *
     * @param page  page number
     * @param size  page size
     * @param group group name
     * @return a mono of list result
     */
    Mono<ListResult<PhotoVo>> list(Integer page, Integer size, String group);
    
    /**
     * List photos by group.
     *
     * @param group group name
     * @return a flux of photo vo
     */
    Flux<PhotoVo> listBy(String group);
    
    /**
     * List all groups.
     *
     * @return a flux of photo group vo
     */
    Flux<PhotoGroupVo> groupBy();
}
