package run.halo.photos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class PhotoQueryTest {

    @Test
    void defaultSortShouldUseEffectiveTimeDescending() {
        var query = query("/photos");

        var orders = query.getSort().toList();
        assertThat(orders).hasSize(3);
        assertThat(orders.get(0).getProperty()).isEqualTo("effectiveTime");
        assertThat(orders.get(0).isDescending()).isTrue();
        assertThat(orders.get(1).getProperty()).isEqualTo("metadata.creationTimestamp");
        assertThat(orders.get(1).isDescending()).isTrue();
        assertThat(orders.get(2).getProperty()).isEqualTo("metadata.name");
        assertThat(orders.get(2).isAscending()).isTrue();
    }

    @Test
    void shootingTimeSortShouldTranslateToEffectiveTimeIndex() {
        var query = query("/photos?sort=exif.dateTimeOriginal,asc");

        var orders = query.getSort().toList();
        assertThat(orders.get(0).getProperty()).isEqualTo("effectiveTime");
        assertThat(orders.get(0).isAscending()).isTrue();
    }

    @Test
    void creationTimeSortShouldRemainFieldSort() {
        var query = query("/photos?sort=metadata.creationTimestamp,asc");

        assertThat(query.getSort().getOrderFor("metadata.creationTimestamp"))
            .extracting(Sort.Order::isAscending)
            .isEqualTo(true);
        assertThat(query.getSort().getOrderFor("effectiveTime")).isNull();
    }

    private static PhotoQuery query(String uri) {
        var request = MockServerHttpRequest.get(uri).build();
        return new PhotoQuery(MockServerWebExchange.from(request));
    }
}
