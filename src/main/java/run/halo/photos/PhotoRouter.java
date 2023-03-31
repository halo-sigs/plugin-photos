package run.halo.photos;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.SettingFetcher;
import run.halo.app.theme.router.strategy.ModelConst;
import run.halo.photos.finders.PhotoFinder;
import run.halo.photos.vo.PhotoGroupVo;

/**
 * Provides a <code>/photos</code> route for the topic end to handle routing.
 *
 * @author LIlGG
 */
@Component
@AllArgsConstructor
public class PhotoRouter {
    
    private PhotoFinder photoFinder;
    
    private final SettingFetcher settingFetcher;
    
    /**
     * Provides a <code>/photos</code> route for the topic end to handle routing.
     *
     * @return a {@link RouterFunction} instance
     */
    @Bean
    RouterFunction<ServerResponse> photoRouter() {
        return route(GET("/photos"), handlerFunction());
    }
    
    private HandlerFunction<ServerResponse> handlerFunction() {
        return request -> ServerResponse.ok().render("photos",
            Map.of("groups", photoGroups(), ModelConst.TEMPLATE_ID, "photos",
                "title", Mono.fromCallable(() -> this.settingFetcher.get(
                    "base").get("title").asText("图库"))
            )
        );
    }
    
    private Mono<List<PhotoGroupVo>> photoGroups() {
        return photoFinder.groupBy().collectList();
    }
}
