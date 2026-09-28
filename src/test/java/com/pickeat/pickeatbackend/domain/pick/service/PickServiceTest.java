package com.pickeat.pickeatbackend.domain.pick.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pickeat.pickeatbackend.domain.member.entity.Member;
import com.pickeat.pickeatbackend.domain.pick.dto.CreatePickRequest;
import com.pickeat.pickeatbackend.domain.pick.dto.PickMapResponse;
import com.pickeat.pickeatbackend.domain.pick.dto.PickPeriod;
import com.pickeat.pickeatbackend.domain.pick.dto.PickResponse;
import com.pickeat.pickeatbackend.domain.pick.dto.PickStatusUpdateRequest;
import com.pickeat.pickeatbackend.domain.pick.dto.RecentPicksResponse;
import com.pickeat.pickeatbackend.domain.pick.entity.Pick;
import com.pickeat.pickeatbackend.domain.pick.entity.PickStatus;
import com.pickeat.pickeatbackend.domain.pick.repository.PickRepository;
import com.pickeat.pickeatbackend.domain.pick.repository.RestaurantPickSummary;
import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
import com.pickeat.pickeatbackend.domain.recommendation.entity.RecommendationSession;
import com.pickeat.pickeatbackend.domain.recommendation.repository.RecommendationCandidateRepository;
import com.pickeat.pickeatbackend.domain.recommendation.repository.RecommendationSessionRepository;
import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import com.pickeat.pickeatbackend.domain.restaurant.repository.RestaurantRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PickServiceTest {

    @Mock PickRepository pickRepository;
    @Mock RecommendationSessionRepository sessionRepository;
    @Mock RecommendationCandidateRepository candidateRepository;
    @Mock RestaurantRepository restaurantRepository;

    private PickService pickService;
    private Member member;
    private Restaurant restaurant;
    private RecommendationSession session;

    @BeforeEach
    void setUp() {
        pickService = new PickService(pickRepository, sessionRepository, candidateRepository, restaurantRepository);
        member = Member.builder().email("user@pickeat.com").password("password").nickname("사용자").build();
        ReflectionTestUtils.setField(member, "id", 1L);
        restaurant = Restaurant.builder().name("테스트 식당").latitude(37.5).longitude(127.0).build();
        ReflectionTestUtils.setField(restaurant, "id", 10L);
        session = RecommendationSession.builder()
                .member(member).companionType(CompanionType.DATE).latitude(37.5).longitude(127.0)
                .foodCategories(Set.of()).build();
        ReflectionTestUtils.setField(session, "id", 20L);
    }

    @Test
    @DisplayName("본인 추천 후보를 Pick으로 생성한다")
    void createsPickFromOwnedCandidate() {
        when(sessionRepository.findByIdAndMemberId(20L, 1L)).thenReturn(Optional.of(session));
        when(candidateRepository.existsBySessionIdAndRestaurantId(20L, 10L)).thenReturn(true);
        when(restaurantRepository.getReferenceById(10L)).thenReturn(restaurant);
        when(pickRepository.saveAndFlush(any(Pick.class))).thenAnswer(invocation -> {
            Pick saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 30L);
            ReflectionTestUtils.setField(saved, "selectedAt", java.time.Instant.now());
            return saved;
        });

        PickResponse response = pickService.create(new CreatePickRequest(20L, 10L), 1L);

        assertThat(response.pickId()).isEqualTo(30L);
        assertThat(response.restaurantId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(PickStatus.SELECTED);
    }

    @Test
    @DisplayName("Pick 생성 시 추천 세션의 동행 유형을 스냅샷으로 저장한다")
    void snapshotsCompanionTypeFromSessionAtCreation() {
        when(sessionRepository.findByIdAndMemberId(20L, 1L)).thenReturn(Optional.of(session));
        when(candidateRepository.existsBySessionIdAndRestaurantId(20L, 10L)).thenReturn(true);
        when(restaurantRepository.getReferenceById(10L)).thenReturn(restaurant);
        ArgumentCaptor<Pick> captor = ArgumentCaptor.forClass(Pick.class);
        when(pickRepository.saveAndFlush(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        pickService.create(new CreatePickRequest(20L, 10L), 1L);

        assertThat(captor.getValue().getCompanionType()).isEqualTo(CompanionType.DATE);
    }

    @Test
    @DisplayName("추천 후보가 아닌 식당은 Pick할 수 없다")
    void rejectsRestaurantOutsideSession() {
        when(sessionRepository.findByIdAndMemberId(20L, 1L)).thenReturn(Optional.of(session));
        when(candidateRepository.existsBySessionIdAndRestaurantId(20L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> pickService.create(new CreatePickRequest(20L, 10L), 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("같은 추천 세션에는 Pick을 중복 생성할 수 없다")
    void rejectsDuplicatePickForSession() {
        when(sessionRepository.findByIdAndMemberId(20L, 1L)).thenReturn(Optional.of(session));
        when(candidateRepository.existsBySessionIdAndRestaurantId(20L, 10L)).thenReturn(true);
        when(pickRepository.existsByRecommendationSessionId(20L)).thenReturn(true);

        assertThatThrownBy(() -> pickService.create(new CreatePickRequest(20L, 10L), 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("다른 사용자의 추천 세션은 존재하지 않는 것처럼 처리한다")
    void hidesAnotherMembersSession() {
        when(sessionRepository.findByIdAndMemberId(20L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pickService.create(new CreatePickRequest(20L, 10L), 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("본인 Pick 상태를 변경한다")
    void updatesOwnedPickStatus() {
        Pick pick = persistedPick(30L);
        when(pickRepository.findByIdAndMemberId(30L, 1L)).thenReturn(Optional.of(pick));

        PickResponse response = pickService.updateStatus(30L, new PickStatusUpdateRequest(PickStatus.REVIEWED), 1L);

        assertThat(response.status()).isEqualTo(PickStatus.REVIEWED);
        assertThat(response.visitedAt()).isNotNull();
    }

    @Test
    @DisplayName("최근 Pick 목록은 식당별로 묶어 개수와 최근 시각을 반환한다")
    void getsMyPicksGroupedByRestaurant() {
        RestaurantPickSummary summary = new RestaurantPickSummary(10L, "테스트 식당", 3L, Instant.now());
        when(pickRepository.findRecentPickSummaries(org.mockito.ArgumentMatchers.eq(1L), any(Instant.class)))
                .thenReturn(List.of(summary));

        RecentPicksResponse response = pickService.getMyPicks(1L, PickPeriod.WEEK);

        assertThat(response.restaurants()).hasSize(1);
        assertThat(response.restaurants().get(0).restaurantId()).isEqualTo(10L);
        assertThat(response.restaurants().get(0).pickCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("최근 Pick 목록 조회는 기간에 맞는 시작 시각을 저장소에 넘긴다")
    void passesPeriodWindowStartToRepository() {
        when(pickRepository.findRecentPickSummaries(org.mockito.ArgumentMatchers.eq(1L), any(Instant.class)))
                .thenReturn(List.of());

        pickService.getMyPicks(1L, PickPeriod.MONTH);

        ArgumentCaptor<Instant> sinceCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(pickRepository).findRecentPickSummaries(org.mockito.ArgumentMatchers.eq(1L), sinceCaptor.capture());
        assertThat(sinceCaptor.getValue()).isCloseTo(
                Instant.now().minus(PickPeriod.MONTH.window()), org.assertj.core.api.Assertions.within(
                        java.time.Duration.ofSeconds(5)));
    }

    @Test
    @DisplayName("지도 조회는 REVIEWED 상태만 노출하도록 저장소에 위임한다")
    void getsMapWithOnlyReviewedPicks() {
        when(pickRepository.findByMemberIdAndStatusOrderBySelectedAtDescIdDesc(1L, PickStatus.REVIEWED))
                .thenReturn(List.of(persistedPick(30L)));

        PickMapResponse response = pickService.getMyPickMap(1L);

        assertThat(response.picks()).hasSize(1);
        verify(pickRepository).findByMemberIdAndStatusOrderBySelectedAtDescIdDesc(1L, PickStatus.REVIEWED);
    }

    private Pick persistedPick(Long id) {
        Pick pick = Pick.builder()
                .member(member).restaurant(restaurant).recommendationSession(session)
                .companionType(CompanionType.DATE).build();
        ReflectionTestUtils.setField(pick, "id", id);
        ReflectionTestUtils.setField(pick, "selectedAt", java.time.Instant.now());
        ReflectionTestUtils.setField(pick, "updatedAt", java.time.Instant.now());
        return pick;
    }
}
