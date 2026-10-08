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
// 치킨·이자카야처럼 두 카테고리에 걸치는 키워드는 보조 카테고리도 함께 돌려준다.
// ponytail: 이름에 키워드가 들어가기만 하면 걸린다. "장어덮밥" 일식집도 한식이 된다. 오분류가 문제가 되면
// 키워드를 빼거나, 특정 Google 유형일 때만 보정하도록 좁힌다.
public final class FoodCategoryNameOverride {

    private static final String RESOURCE = "/curated/category-name-keywords.txt";

    // 긴 키워드가 먼저, 길이가 같으면 파일에 먼저 적힌 것이 먼저 오도록 정렬해 둔다.
    private static final List<Keyword> KEYWORDS = load();

    private FoodCategoryNameOverride() {
    }

    public static Optional<Categories> find(String restaurantName) {
        if (restaurantName == null || restaurantName.isBlank()) {
            return Optional.empty();
        }
        String name = normalize(restaurantName);
        return KEYWORDS.stream()
                .filter(keyword -> name.contains(keyword.text()))
                .map(Keyword::categories)
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
                        Categories categories = parseCategories(line.substring(0, separator));
                        for (String raw : line.substring(separator + 1).split(",")) {
                            String text = normalize(raw);
                            if (!text.isEmpty()) {
                                keywords.add(new Keyword(text, categories));
                            }
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // List.sort는 안정 정렬이라 길이가 같은 키워드는 파일 순서를 유지한다.
        keywords.sort(Comparator.comparingInt((Keyword keyword) -> keyword.text().length()).reversed());
        return List.copyOf(keywords);
    }

    private static Categories parseCategories(String value) {
        String[] parts = value.strip().split("\\+");
        FoodCategory primary = FoodCategory.valueOf(parts[0].strip());
        FoodCategory secondary = parts.length > 1 ? FoodCategory.valueOf(parts[1].strip()) : null;
        return new Categories(primary, secondary);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    // secondary는 없으면 null이다.
    public record Categories(FoodCategory primary, FoodCategory secondary) {
    }

    private record Keyword(String text, Categories categories) {
    }
}
