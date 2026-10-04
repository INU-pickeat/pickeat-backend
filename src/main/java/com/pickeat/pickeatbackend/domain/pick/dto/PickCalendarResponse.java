package com.pickeat.pickeatbackend.domain.pick.dto;

import com.pickeat.pickeatbackend.domain.pick.entity.Pick;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record PickCalendarResponse(int year, int month, List<DateItem> dates) {

    // picks는 visitedAt 오름차순이어야 한다. 날짜별 첫 기록의 식당 이름을 대표로 쓴다.
    public static PickCalendarResponse of(YearMonth yearMonth, List<Pick> picks, ZoneId zone) {
        Map<LocalDate, List<Pick>> byDate = new LinkedHashMap<>();
        for (Pick pick : picks) {
            LocalDate date = pick.getVisitedAt().atZone(zone).toLocalDate();
            byDate.computeIfAbsent(date, ignored -> new ArrayList<>()).add(pick);
        }
        List<DateItem> dates = byDate.entrySet().stream()
                .map(entry -> DateItem.of(entry.getKey(), entry.getValue()))
                .toList();
        return new PickCalendarResponse(yearMonth.getYear(), yearMonth.getMonthValue(), dates);
    }

    public record DateItem(
            LocalDate date,
            int recordCount,
            String restaurantName,
            String representativeImageUrl
    ) {
        private static DateItem of(LocalDate date, List<Pick> picksOfDate) {
            // 대표 이미지는 그날 가장 먼저 등록된 후기 사진이다. Review 이미지 기능이 생기기 전까지는 null이다.
            return new DateItem(date, picksOfDate.size(), picksOfDate.get(0).getRestaurant().getName(), null);
        }
    }
}
