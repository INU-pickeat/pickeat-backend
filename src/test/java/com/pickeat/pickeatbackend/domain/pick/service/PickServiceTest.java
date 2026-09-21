package com.pickeat.pickeatbackend.domain.pick.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pickeat.pickeatbackend.domain.member.entity.Member;
import com.pickeat.pickeatbackend.domain.pick.dto.CreatePickRequest;
import com.pickeat.pickeatbackend.domain.pick.dto.PickListResponse;
import com.pickeat.pickeatbackend.domain.pick.dto.PickMapResponse;
import com.pickeat.pickeatbackend.domain.pick.dto.PickResponse;
import com.pickeat.pickeatbackend.domain.pick.dto.PickStatusUpdateRequest;
import com.pickeat.pickeatbackend.domain.pick.entity.Pick;
import com.pickeat.pickeatbackend.domain.pick.entity.PickStatus;
import com.pickeat.pickeatbackend.domain.pick.repository.PickRepository;
import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
import com.pickeat.pickeatbackend.domain.recommendation.entity.RecommendationSession;
import com.pickeat.pickeatbackend.domain.recommendation.repository.RecommendationCandidateRepository;
import com.pickeat.pickeatbackend.domain.recommendation.repository.RecommendationSessionRepository;
import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import com.pickeat.pickeatbackend.domain.restaurant.repository.RestaurantRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
    @DisplayName("Pick 목록은 최신 선택순 페이지 조건으로 조회한다")
    void getsMyPicksWithStableNewestFirstSort() {
        Pick pick = persistedPick(30L);
        when(pickRepository.findByMemberId(any(Long.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pick)));

        PickListResponse response = pickService.getMyPicks(1L, 0, 20);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(pickRepository).findByMemberId(org.mockito.ArgumentMatchers.eq(1L), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("selectedAt").isDescending()).isTrue();
        assertThat(captor.getValue().getSort().getOrderFor("id").isDescending()).isTrue();
        assertThat(response.items()).hasSize(1);
    }

    @Test
    @DisplayName("지도 조회는 취소 상태를 제외하도록 저장소에 위임한다")
    void getsMapWithoutCanceledPicks() {
        when(pickRepository.findByMemberIdAndStatusNotOrderBySelectedAtDescIdDesc(1L, PickStatus.CANCELED))
                .thenReturn(List.of(persistedPick(30L)));

        PickMapResponse response = pickService.getMyPickMap(1L);

        assertThat(response.picks()).hasSize(1);
        verify(pickRepository).findByMemberIdAndStatusNotOrderBySelectedAtDescIdDesc(1L, PickStatus.CANCELED);
    }

    private Pick persistedPick(Long id) {
        Pick pick = Pick.builder().member(member).restaurant(restaurant).recommendationSession(session).build();
        ReflectionTestUtils.setField(pick, "id", id);
        ReflectionTestUtils.setField(pick, "selectedAt", java.time.Instant.now());
        ReflectionTestUtils.setField(pick, "updatedAt", java.time.Instant.now());
        return pick;
    }
}
