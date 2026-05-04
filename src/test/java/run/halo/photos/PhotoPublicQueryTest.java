package run.halo.photos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class PhotoPublicQueryTest {

    @Test
    void shouldParseAllQueryParams() {
        var query = query("/photos?group=trips&ungrouped=true&tag=sunset&keyword=beach&sort=metadata.creationTimestamp,asc&page=2&size=20");

        assertThat(query.getGroup()).isEqualTo("trips");
        assertThat(query.isUngrouped()).isTrue();
        assertThat(query.getTag()).isEqualTo("sunset");
        assertThat(query.getKeyword()).isEqualTo("beach");
        assertThat(query.getPage()).isEqualTo(2);
        assertThat(query.getSize()).isEqualTo(20);
        assertThat(query.isEffectiveTimeSort()).isFalse();
        assertThat(query.getSort().getOrderFor("metadata.creationTimestamp"))
            .extracting(Sort.Order::isAscending)
            .isEqualTo(true);
    }

    @Test
    void defaultSortShouldUseEffectiveTimeDescending() {
        var query = query("/photos");

        assertThat(query.isEffectiveTimeSort()).isTrue();
        assertThat(query.isEffectiveTimeAscending()).isFalse();
    }

    private static PhotoPublicQuery query(String uri) {
        var request = MockServerHttpRequest.get(uri).build();
        return new PhotoPublicQuery(MockServerWebExchange.from(request));
    }
}
