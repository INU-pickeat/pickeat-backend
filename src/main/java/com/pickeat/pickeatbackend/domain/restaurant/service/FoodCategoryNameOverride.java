package com.pickeat.pickeatbackend.domain.restaurant.service;

import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

// Google 유형이 실제 음식과 다를 때 식당 이름으로 카테고리를 보정한다. Google은 샤브칼국수·장어집을
// japanese_restaurant로, 양꼬치집을 일식 꼬치로 주는 경우가 있다. 이름에 키워드가 있으면 그 카테고리를 우선한다.
// ponytail: 이름에 키워드가 들어가기만 하면 걸린다. "장어덮밥" 일식집도 한식이 된다. 오분류가 문제가 되면
// 키워드를 빼거나, 특정 Google 유형일 때만 보정하도록 좁힌다.
public final class FoodCategoryNameOverride {

    private static final String RESOURCE = "/curated/category-name-keywords.txt";

    // 긴 키워드가 먼저 오도록 정렬해 둔다.
    private static final List<Keyword> KEYWORDS = load();

    private FoodCategoryNameOverride() {
    }

    public static Optional<FoodCategory> find(String restaurantName) {
        if (restaurantName == null || restaurantName.isBlank()) {
            return Optional.empty();
        }
        String name = normalize(restaurantName);
        return KEYWORDS.stream()
                .filter(keyword -> name.contains(keyword.text()))
                .map(Keyword::category)
                .findFirst();
    }

    private static List<Keyword> load() {
        List<Keyword> keywords = new ArrayList<>();
        try (InputStream input = FoodCategoryNameOverride.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("카테고리 보정 키워드 목록을 찾을 수 없습니다: " + RESOURCE);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            reader.lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .forEach(line -> {
                        int separator = line.indexOf(':');
                        if (separator < 0) {
                            throw new IllegalStateException("카테고리 보정 키워드 형식이 잘못됐습니다: " + line);
                        }
                        FoodCategory category = FoodCategory.valueOf(line.substring(0, separator).strip());
                        for (String raw : line.substring(separator + 1).split(",")) {
                            String text = normalize(raw);
                            if (!text.isEmpty()) {
                                keywords.add(new Keyword(text, category));
                            }
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        keywords.sort(Comparator.comparingInt((Keyword keyword) -> keyword.text().length()).reversed());
        return List.copyOf(keywords);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private record Keyword(String text, FoodCategory category) {
    }
}
