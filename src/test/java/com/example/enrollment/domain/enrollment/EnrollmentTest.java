package com.example.enrollment.domain.enrollment;

import com.example.enrollment.common.exception.EnrollmentCancelNotAllowedException;
import com.example.enrollment.common.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Enrollment 도메인 단위 테스트")
class EnrollmentTest {

    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        enrollment = Enrollment.builder()
                .courseId(1L)
                .userId(1L)
                .build();
    }

    @Nested
    @DisplayName("confirm() 기능 테스트")
    class ConfirmTest {

        @Test
        @DisplayName("PENDING 상태에서 결제 확정 시 CONFIRMED로 변경되고 confirmedAt이 설정된다.")
        void confirm_success() {
            // [When]
            enrollment.confirm();

            // [Then]
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
            assertThat(enrollment.getConfirmedAt()).isNotNull();
            assertThat(enrollment.getConfirmedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
        }

        @Test
        @DisplayName("CONFIRMED 상태에서 결제 확정 시 예외 발생")
        void confirm_fail_alreadyConfirmed() {
            // [Given]
            enrollment.confirm();

            // [When & Then]
            assertThatThrownBy(() -> enrollment.confirm())
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        @DisplayName("CANCELLED 상태에서 결제 확정 시 예외 발생")
        void confirm_fail_alreadyCancelled() {
            // [Given]
            enrollment.cancel();

            // [When & Then]
            assertThatThrownBy(() -> enrollment.confirm())
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("cancel() 기능 테스트")
    class CancelTest {

        @Test
        @DisplayName("PENDING 상태에서 취소 시 CANCELLED로 변경되고 cancelledAt이 설정된다.")
        void cancel_success_fromPending() {
            // [When]
            enrollment.cancel();

            // [Then]
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
            assertThat(enrollment.getCancelledAt()).isNotNull();
        }

        @Test
        @DisplayName("결제 확정 후 6일 후 취소 시 취소 가능하다.")
        void isCancellable_6DaysAfter() {
            // [Given]
            enrollment.confirm();
            ReflectionTestUtils.setField(enrollment, "confirmedAt", LocalDateTime.now().minusDays(6));

            // [When & Then]
            assertThat(enrollment.isCancellable()).isTrue();
        }

        @Test
        @DisplayName("결제 확정 후 7일 당일 취소 시 취소 가능하다.")
        void isCancellable_exactly7Days() {
            // [Given]
            enrollment.confirm();
            ReflectionTestUtils.setField(enrollment, "confirmedAt", LocalDateTime.now().minusDays(7));

            // [When & Then]
            assertThat(enrollment.isCancellable()).isTrue();
        }

        @Test
        @DisplayName("결제 확정 후 7일 초과 시 취소하면 예외 발생")
        void cancel_fail_afterCancellableWindow() {
            enrollment.confirm();
            ReflectionTestUtils.setField(enrollment, "confirmedAt", LocalDateTime.now().minusDays(8));

            assertThatThrownBy(() -> enrollment.cancel())
                    .isInstanceOf(EnrollmentCancelNotAllowedException.class);
        }

        @Test
        @DisplayName("CANCELLED 상태에서 재취소 시 예외 발생")
        void cancel_fail_alreadyCancelled() {
            // [Given]
            enrollment.cancel();

            // [When & Then]
            assertThatThrownBy(() -> enrollment.cancel())
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        @DisplayName("결제 확정 후 7일 이내에 취소 시 CANCELLED로 변경된다.")
        void cancel_success_fromConfirmed() {
            enrollment.confirm();
            enrollment.cancel();

            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
            assertThat(enrollment.getCancelledAt()).isNotNull();
        }

    }
}