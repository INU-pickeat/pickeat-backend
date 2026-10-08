package com.pickeat.pickeatbackend.domain.restaurant.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

// 데이트 추천에서 제외할 프랜차이즈를 식당 이름으로 판별한다. Google Places에는 프랜차이즈 여부 필드가
// 없어서 팀이 정리한 브랜드 목록(curated/franchise-brands.txt)과 이름을 비교한다.
// 오탐을 줄이려고 짧은 브랜드는 첫 단어가 정확히 같을 때만 프랜차이즈로 본다.
// ponytail: 이름 접두사 비교라 "본가 ○○"처럼 짧은 브랜드와 첫 단어가 같은 개인 식당은 걸러지고,
// 영문·변형 표기는 목록에 별칭을 추가해야 잡힌다. 오탐이 문제가 되면 restaurants에 브랜드 컬럼을 두고 upsert 때 태깅한다.
public final class FranchiseBrands {

    private static final String RESOURCE = "/curated/franchise-brands.txt";
    private static final int PREFIX_MATCH_MIN_LENGTH = 4;
    private static final int SHORT_BRAND_JOINED_LENGTH = 3;
    private static final String BRANCH_SUFFIX = "점";

    private static final Set<String> PREFIX_BRANDS = new HashSet<>();
    private static final Set<String> FIRST_WORD_BRANDS = new HashSet<>();

    static {
        try (InputStream input = FranchiseBrands.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("프랜차이즈 브랜드 목록을 찾을 수 없습니다: " + RESOURCE);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            reader.lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(FranchiseBrands::normalize)
                    .filter(brand -> !brand.isEmpty())
                    .forEach(brand -> (brand.length() >= PREFIX_MATCH_MIN_LENGTH ? PREFIX_BRANDS : FIRST_WORD_BRANDS)
                            .add(brand));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private FranchiseBrands() {
    }

    public static boolean isFranchise(String restaurantName) {
        if (restaurantName == null || restaurantName.isBlank()) {
            return false;
        }
        String firstWord = normalize(restaurantName.strip().split("\\s+")[0]);
        if (FIRST_WORD_BRANDS.contains(firstWord)) {
            return true;
        }
        String name = normalize(restaurantName);
        if (PREFIX_BRANDS.stream().anyMatch(name::startsWith)) {
            return true;
        }
        // "투다리라이온점"처럼 3글자 브랜드에 지점명을 붙여 쓴 이름. 2글자 브랜드는 오탐이 많아 적용하지 않는다.
        return name.endsWith(BRANCH_SUFFIX) && FIRST_WORD_BRANDS.stream()
                .anyMatch(brand -> brand.length() == SHORT_BRAND_JOINED_LENGTH && name.startsWith(brand));
    }

    // 공백·기호를 지우고 소문자로 바꾼다. "본죽&비빔밥 홍대점" → "본죽비빔밥홍대점"
    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }
}
