package com.pickeat.pickeatbackend.domain.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickeat.pickeatbackend.domain.discovery.entity.DiscoveryRegion;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// V15가 대표 이미지 URL을 "/images/discovery/{지역 코드 소문자}_{순서}_main.jpg"로 만든다.
// 파일 이름이 지역 코드와 다르면 운영에서 404가 난다(서촌: 코드와 파일 이름이 SEOCHEON·seochon_으로 어긋났었다).
class DiscoveryImageResourceTest {

    @ParameterizedTest
    @EnumSource(DiscoveryRegion.class)
    @DisplayName("지역마다 5개 대표 이미지 파일이 지역 코드 이름으로 존재한다")
    void hasImageFilesNamedAfterRegionCode(DiscoveryRegion region) {
        for (int order = 1; order <= 5; order++) {
            String path = "/static/images/discovery/%s_%02d_main.jpg"
                    .formatted(region.name().toLowerCase(Locale.ROOT), order);
            assertThat(getClass().getResource(path)).as(path).isNotNull();
        }
    }
}
