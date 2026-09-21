package com.pickeat.pickeatbackend.domain.pick.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pickeat.pickeatbackend.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PickTest {

    @Test
    @DisplayName("Pick은 SELECTED 상태로 생성된다")
    void startsSelected() {
        Pick pick = Pick.builder().build();

        assertThat(pick.getStatus()).isEqualTo(PickStatus.SELECTED);
        assertThat(pick.getVisitedAt()).isNull();
    }

    @Test
    @DisplayName("SELECTED에서 REVIEWED로 바꾸면 방문 시각이 기록된다")
    void changesSelectedToReviewed() {
        Pick pick = Pick.builder().build();

        pick.changeStatus(PickStatus.REVIEWED);

        assertThat(pick.getStatus()).isEqualTo(PickStatus.REVIEWED);
        assertThat(pick.getVisitedAt()).isNotNull();
    }

    @Test
    @DisplayName("SELECTED에서 CANCELED로 바꿀 수 있다")
    void changesSelectedToCanceled() {
        Pick pick = Pick.builder().build();

        pick.changeStatus(PickStatus.CANCELED);

        assertThat(pick.getStatus()).isEqualTo(PickStatus.CANCELED);
        assertThat(pick.getVisitedAt()).isNull();
    }

    @Test
    @DisplayName("같은 상태 요청은 멱등 처리한다")
    void acceptsSameStatusIdempotently() {
        Pick pick = Pick.builder().build();

        pick.changeStatus(PickStatus.SELECTED);

        assertThat(pick.getStatus()).isEqualTo(PickStatus.SELECTED);
    }

    @Test
    @DisplayName("종료 상태에서는 다른 상태로 변경할 수 없다")
    void rejectsTransitionFromTerminalState() {
        Pick pick = Pick.builder().build();
        pick.changeStatus(PickStatus.REVIEWED);

        assertThatThrownBy(() -> pick.changeStatus(PickStatus.CANCELED))
                .isInstanceOf(BusinessException.class);
    }
}
