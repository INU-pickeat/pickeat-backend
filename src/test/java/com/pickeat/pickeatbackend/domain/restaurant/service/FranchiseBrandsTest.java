package com.pickeat.pickeatbackend.domain.restaurant.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class FranchiseBrandsTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "스타벅스 홍대역점",
            "스타벅스홍대역점",
            "Starbucks",
            "교촌치킨 연남점",
            "BBQ치킨 망원점",
            "bhc 합정점",
            "KFC 신촌점",
            "본죽&비빔밥 홍대점",
            "아웃백 스테이크하우스 합정점",
            "홍콩반점0410 연남점",
            "본가 홍대점",
            "두끼 신촌점",
            "투다리 복돼지점",
            "투다리라이온점"
    })
    @DisplayName("목록에 있는 브랜드로 시작하는 이름은 프랜차이즈로 본다")
    void detectsFranchiseNames(String name) {
        assertThat(FranchiseBrands.isFranchise(name)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "오레노라멘 본점",
            "연하동 연남본점",
            "풍천장어 연남점",
            "본가네 감자탕",
            "두끼니 식당",
            "본가네감자탕 부천점",
            "스시로바 하루",
            "연남동 스타벅스 옆 파스타"
    })
    @DisplayName("브랜드로 시작하지 않거나 짧은 브랜드와 첫 단어가 다르면 프랜차이즈가 아니다")
    void ignoresIndependentRestaurants(String name) {
        assertThat(FranchiseBrands.isFranchise(name)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "폴바셋 홍대점",
            "고든램지 스트리트 버거",
            "깐부치킨 연남점",
            "청기와타운 합정점",
            "미카도스시 홍대점",
            "상무초밥 상암점",
            "은행골 본점"
    })
    @DisplayName("맛집으로 인식되는 예외 브랜드는 프랜차이즈로 보지 않는다")
    void keepsExceptionBrands(String name) {
        assertThat(FranchiseBrands.isFranchise(name)).isFalse();
    }

    // 2026-10-21 팀 전달 목록. 판정 목록에는 별칭(Starbucks, 메가커피 등)이 더 있으므로 개수가 아니라 판정 결과로 확인한다.
    @ParameterizedTest
    @MethodSource("requestedBrands")
    @DisplayName("팀이 전달한 프랜차이즈 브랜드는 지점명을 붙여도 모두 프랜차이즈로 본다")
    void detectsEveryRequestedBrand(String brand) {
        assertThat(FranchiseBrands.isFranchise(brand + " 테스트점")).isTrue();
    }

    static Stream<String> requestedBrands() throws IOException {
        try (InputStream input = FranchiseBrandsTest.class.getResourceAsStream("/curated/requested-franchise-brands.txt")) {
            assertThat(input).isNotNull();
            List<String> brands = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)).lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .toList();
            return brands.stream();
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("이름이 없으면 프랜차이즈가 아니다")
    void treatsMissingNameAsIndependent(String name) {
        assertThat(FranchiseBrands.isFranchise(name)).isFalse();
    }
}
