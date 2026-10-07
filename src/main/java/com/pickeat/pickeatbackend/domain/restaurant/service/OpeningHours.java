package com.pickeat.pickeatbackend.domain.restaurant.service;

import com.pickeat.pickeatbackend.domain.restaurant.client.GooglePlaceResponse;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

// 영업시간은 한 주를 분 단위(0 = 일요일 0시, 10080 = 다음 일요일 0시)로 펼친 [여는 분, 닫는 분] 쌍으로 다룬다.
// 토요일 밤에 열어 일요일 새벽에 닫으면 닫는 분이 여는 분보다 작다.
public final class OpeningHours {

    private static final int MINUTES_PER_DAY = 24 * 60;
    private static final int MINUTES_PER_WEEK = 7 * MINUTES_PER_DAY;

    private OpeningHours() {
    }

    public static int[] toWeekMinutes(GooglePlaceResponse.OpeningHours openingHours) {
        if (openingHours == null || openingHours.periods() == null || openingHours.periods().isEmpty()) {
            return null;
        }
        List<Integer> minutes = new ArrayList<>();
        for (GooglePlaceResponse.Period period : openingHours.periods()) {
            if (period.open() == null) {
                continue;
            }
            if (period.close() == null) {
                return new int[] {0, MINUTES_PER_WEEK};
            }
            minutes.add(weekMinute(period.open()));
            minutes.add(weekMinute(period.close()));
        }
        return minutes.isEmpty() ? null : minutes.stream().mapToInt(Integer::intValue).toArray();
    }

    // 영업시간을 모르면 영업 중으로 본다. 닫는 시각 정각은 영업이 끝난 것으로 본다.
    public static boolean isOpenAt(int[] weekMinutes, ZonedDateTime time) {
        if (weekMinutes == null || weekMinutes.length == 0) {
            return true;
        }
        int now = (time.getDayOfWeek().getValue() % 7) * MINUTES_PER_DAY + time.getHour() * 60 + time.getMinute();
        for (int i = 0; i + 1 < weekMinutes.length; i += 2) {
            int open = weekMinutes[i];
            int close = weekMinutes[i + 1];
            boolean openNow = open <= close ? open <= now && now < close : now >= open || now < close;
            if (openNow) {
                return true;
            }
        }
        return false;
    }

    private static int weekMinute(GooglePlaceResponse.Point point) {
        return nullToZero(point.day()) * MINUTES_PER_DAY + nullToZero(point.hour()) * 60 + nullToZero(point.minute());
    }

    private static int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
