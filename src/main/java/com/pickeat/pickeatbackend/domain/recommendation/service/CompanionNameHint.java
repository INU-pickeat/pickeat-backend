package com.pickeat.pickeatbackend.domain.recommendation.service;

import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
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

// 식당 이름의 메뉴 키워드로 동행 적합도를 추정한다. 팀이 입력한 적합도가 없는 식당에만 쓰는 보조 신호다.
// 조명·뷰·좌석처럼 메뉴로 알 수 없는 특징은 판단하지 않는다. 키워드가 없으면 빈 값(모름)이다.
public final class CompanionNameHint {

    private static final String RESOURCE = "/curated/companion-name-keywords.txt";

    private static final List<Keyword> KEYWORDS = load();

    private CompanionNameHint() {
    }

    // true = 적합, false = 부적합, 빈 값 = 모름
    public static Optional<Boolean> find(String restaurantName, CompanionType companionType) {
        if (restaurantName == null || restaurantName.isBlank() || companionType == null) {
            return Optional.empty();
        }
        String name = normalize(restaurantName);
        // 가장 긴 키워드를, 길이가 같으면 이름에서 더 뒤에 나오는 키워드를 따른다. 우리말 상호는 주메뉴가 뒤에 온다.
        return KEYWORDS.stream()
                .filter(keyword -> keyword.companionType() == companionType && name.contains(keyword.text()))
                .max(Comparator.comparingInt((Keyword keyword) -> keyword.text().length())
                        .thenComparingInt(keyword -> name.lastIndexOf(keyword.text())))
                .map(Keyword::suitable);
    }

    private static List<Keyword> load() {
        List<Keyword> keywords = new ArrayList<>();
        try (InputStream input = CompanionNameHint.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("동행 적합도 키워드 목록을 찾을 수 없습니다: " + RESOURCE);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            reader.lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .forEach(line -> {
                        int separator = line.indexOf(':');
                        String[] head = separator < 0 ? new String[0] : line.substring(0, separator).strip().split("\\s+");
                        if (head.length != 2 || !(head[1].equals("GOOD") || head[1].equals("BAD"))) {
                            throw new IllegalStateException("동행 적합도 키워드 형식이 잘못됐습니다: " + line);
                        }
                        CompanionType companionType = CompanionType.valueOf(head[0]);
                        boolean suitable = head[1].equals("GOOD");
                        for (String raw : line.substring(separator + 1).split(",")) {
                            String text = normalize(raw);
                            if (!text.isEmpty()) {
                                keywords.add(new Keyword(text, companionType, suitable));
                            }
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return List.copyOf(keywords);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private record Keyword(String text, CompanionType companionType, boolean suitable) {
    }
}
