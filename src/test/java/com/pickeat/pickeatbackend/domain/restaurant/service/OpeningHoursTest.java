package com.pickeat.pickeatbackend.domain.restaurant.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickeat.pickeatbackend.domain.restaurant.client.GooglePlaceResponse;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OpeningHoursTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    // 2026-10-07은 수요일(Google day 3)이다.
    private static ZonedDateTime wednesday(int hour, int minute) {
        return ZonedDateTime.of(2026, 10, 7, hour, minute, 0, 0, SEOUL);
    }

    private static GooglePlaceResponse.Period period(int openDay, int openHour, int closeDay, int closeHour) {
        return new GooglePlaceResponse.Period(
                new GooglePlaceResponse.Point(openDay, openHour, 0),
                new GooglePlaceResponse.Point(closeDay, closeHour, 0));
    }

    @Test
    @DisplayName("Google 영업시간을 한 주 분 단위 구간으로 바꾼다")
    void convertsGooglePeriodsToWeekMinutes() {
        int[] minutes = OpeningHours.toWeekMinutes(
                new GooglePlaceResponse.OpeningHours(List.of(period(3, 11, 3, 21))));

        assertThat(minutes).containsExactly(3 * 1440 + 11 * 60, 3 * 1440 + 21 * 60);
    }

    @Test
    @DisplayName("닫는 시각이 없으면 24시간 영업으로 본다")
    void treatsMissingCloseAsAlwaysOpen() {
        int[] minutes = OpeningHours.toWeekMinutes(new GooglePlaceResponse.OpeningHours(List.of(
                new GooglePlaceResponse.Period(new GooglePlaceResponse.Point(0, 0, 0), null))));

        assertThat(OpeningHours.isOpenAt(minutes, wednesday(3, 0))).isTrue();
    }

    @Test
    @DisplayName("영업시간 정보가 없으면 null이고 영업 중으로 본다")
    void treatsUnknownHoursAsOpen() {
        assertThat(OpeningHours.toWeekMinutes(null)).isNull();
        assertThat(OpeningHours.toWeekMinutes(new GooglePlaceResponse.OpeningHours(List.of()))).isNull();
        assertThat(OpeningHours.isOpenAt(null, wednesday(3, 0))).isTrue();
    }

    @Test
    @DisplayName("여는 시각은 포함하고 닫는 시각 정각은 영업 종료로 본다")
    void includesOpeningMinuteAndExcludesClosingMinute() {
        int[] minutes = OpeningHours.toWeekMinutes(
                new GooglePlaceResponse.OpeningHours(List.of(period(3, 11, 3, 21))));

        assertThat(OpeningHours.isOpenAt(minutes, wednesday(10, 59))).isFalse();
        assertThat(OpeningHours.isOpenAt(minutes, wednesday(11, 0))).isTrue();
        assertThat(OpeningHours.isOpenAt(minutes, wednesday(20, 59))).isTrue();
        assertThat(OpeningHours.isOpenAt(minutes, wednesday(21, 0))).isFalse();
    }

    @Test
    @DisplayName("브레이크타임에는 영업하지 않는 것으로 본다")
    void excludesBreakTime() {
        int[] minutes = OpeningHours.toWeekMinutes(new GooglePlaceResponse.OpeningHours(List.of(
                period(3, 11, 3, 15), period(3, 17, 3, 22))));

        assertThat(OpeningHours.isOpenAt(minutes, wednesday(16, 0))).isFalse();
        assertThat(OpeningHours.isOpenAt(minutes, wednesday(18, 0))).isTrue();
    }

    @Test
    @DisplayName("자정을 넘겨 영업하면 다음 날 새벽에도 영업 중이다")
    void handlesOvernightHours() {
        // 화요일 18시 ~ 수요일 2시
        int[] minutes = OpeningHours.toWeekMinutes(
                new GooglePlaceResponse.OpeningHours(List.of(period(2, 18, 3, 2))));

        assertThat(OpeningHours.isOpenAt(minutes, wednesday(1, 30))).isTrue();
        assertThat(OpeningHours.isOpenAt(minutes, wednesday(2, 30))).isFalse();
    }

    @Test
    @DisplayName("토요일 밤에 열어 일요일 새벽에 닫는 영업도 처리한다")
    void handlesSaturdayToSundayWrap() {
        int[] minutes = OpeningHours.toWeekMinutes(
                new GooglePlaceResponse.OpeningHours(List.of(period(6, 22, 0, 3))));
        ZonedDateTime sundayOneAm = ZonedDateTime.of(2026, 10, 11, 1, 0, 0, 0, SEOUL);
        ZonedDateTime saturdayElevenPm = ZonedDateTime.of(2026, 10, 10, 23, 0, 0, 0, SEOUL);

        assertThat(OpeningHours.isOpenAt(minutes, sundayOneAm)).isTrue();
        assertThat(OpeningHours.isOpenAt(minutes, saturdayElevenPm)).isTrue();
        assertThat(OpeningHours.isOpenAt(minutes, wednesday(12, 0))).isFalse();
    }
}
