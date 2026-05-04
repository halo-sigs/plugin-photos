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

        assertThat(query.isEffectiveTimeSort()).isTrue();
        assertThat(query.isEffectiveTimeAscending()).isFalse();
    }

    @Test
    void shootingTimeSortShouldUseEffectiveTimeDirection() {
        var query = query("/photos?sort=exif.dateTimeOriginal,asc");

        assertThat(query.isEffectiveTimeSort()).isTrue();
        assertThat(query.isEffectiveTimeAscending()).isTrue();
    }

    @Test
    void creationTimeSortShouldRemainFieldSort() {
        var query = query("/photos?sort=metadata.creationTimestamp,asc");

        assertThat(query.isEffectiveTimeSort()).isFalse();
        assertThat(query.getSort().getOrderFor("metadata.creationTimestamp"))
            .extracting(Sort.Order::isAscending)
            .isEqualTo(true);
    }

    private static PhotoQuery query(String uri) {
        var request = MockServerHttpRequest.get(uri).build();
        return new PhotoQuery(MockServerWebExchange.from(request));
    }
}
