package com.example.enrollment.application;

import com.example.enrollment.common.exception.*;
import com.example.enrollment.domain.course.Course;
import com.example.enrollment.domain.course.CourseRepository;
import com.example.enrollment.domain.enrollment.Enrollment;
import com.example.enrollment.domain.enrollment.EnrollmentRepository;
import com.example.enrollment.domain.enrollment.EnrollmentStatus;
import com.example.enrollment.presentation.dto.response.EnrollmentResponse;
import com.example.enrollment.presentation.dto.response.EnrollmentWithWaitCountResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/*==================Service 테스트 코드 작성 규칙===========================
1. 테스트 대상 Class 내부 메소드 외의 의존성은 Mock 주입을 통해 동작 정의
2. 테스트에 사용되는 중복되는 파라미터 및 Entity는 클래스 영역에 정의
3. 테스트 코드 작성 양식은 [Given], [When], [Then] 양식으로 작성
    3.1 [Given] 테스트에 필요한 파라미터, Mock 동작 정의
    3.2 [When] 테스트에 대한 상황을 정의
    3.3 [Then] 동작 이후 결과를 검증
    3.4 상황과 동시에 동작 결과가 발생하는 경우 [When & Then]을 통해 정의
4. verify를 통한 메소드의 동작 여부 또한 검증
 */

@ExtendWith(MockitoExtension.class)
public class EnrollmentServiceTest {

    @InjectMocks
    private EnrollmentService enrollmentService;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    private static final List<EnrollmentStatus> ACTIVE_STATUSES =
            List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED);

    private final Long userId = 1L;
    private final Long courseId = 10L;
    private final Long enrollmentId = 100L;

    private Course course;
    private Enrollment enrollment;
    private Enrollment pendingEnrollment;
    private Enrollment confirmedEnrollment;
    private Enrollment waitingEnrollment;


    @BeforeEach
    void setUp() {
        course = Course.builder()
                .title("테스트 강의")
                .description("강의 설명")
                .price(BigDecimal.valueOf(50000))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .maxCapacity(30)
                .instructorId(1L)
                .build();
        ReflectionTestUtils.setField(course, "id", courseId);

        enrollment = Enrollment.builder()
                .courseId(courseId)
                .userId(userId)
                .build();
        ReflectionTestUtils.setField(enrollment, "id", enrollmentId);

        pendingEnrollment = Enrollment.builder()
                .courseId(courseId)
                .userId(userId)
                .build();
        ReflectionTestUtils.setField(pendingEnrollment, "id", enrollmentId);

        confirmedEnrollment = Enrollment.builder()
                .courseId(courseId)
                .userId(userId)
                .build();
        ReflectionTestUtils.setField(confirmedEnrollment, "id", 2L);
        ReflectionTestUtils.setField(confirmedEnrollment, "status", EnrollmentStatus.CONFIRMED);
        ReflectionTestUtils.setField(confirmedEnrollment, "confirmedAt", LocalDateTime.now());

        waitingEnrollment = Enrollment.reserve(userId, courseId, 1);
        ReflectionTestUtils.setField(waitingEnrollment, "id", 3L);
    }

    @Nested
    @DisplayName("enroll() 기능 테스트")
    class EnrollTest {

        @Test
        @DisplayName("수강 신청 성공")
        void enroll_success() {
            // [Given]
            course.open();
            given(courseRepository.findByIdWithLock(courseId)).willReturn(Optional.of(course));
            given(enrollmentRepository.existsByCourseIdAndUserIdAndStatusIn(courseId, userId, ACTIVE_STATUSES)).willReturn(false);
            given(enrollmentRepository.countByCourseIdAndStatusIn(courseId, ACTIVE_STATUSES)).willReturn(0);
            given(enrollmentRepository.save(any(Enrollment.class))).willAnswer(invocation -> {
                Enrollment saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", enrollmentId);
                return saved;
            });

            // [When]
            EnrollmentResponse result = enrollmentService.enroll(courseId, userId);

            // [Then]
            assertThat(result).isNotNull();
            assertThat(result.courseId()).isEqualTo(courseId);
            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.status()).isEqualTo(EnrollmentStatus.PENDING);

            verify(courseRepository, times(1)).findByIdWithLock(courseId);
            verify(enrollmentRepository, times(1)).existsByCourseIdAndUserIdAndStatusIn(courseId, userId, ACTIVE_STATUSES);
            verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
        }

        @Test
        @DisplayName("max_capacity가 null인 경우 정원 체크 없이 수강 신청 성공")
        void enroll_success_unlimitedCapacity() {
            // [Given]
            Course unlimitedCourse = Course.builder()
                    .title("무제한 강의")
                    .description("설명")
                    .price(BigDecimal.valueOf(10000))
                    .instructorId(1L)
                    .build();
            ReflectionTestUtils.setField(unlimitedCourse, "id", courseId);
            unlimitedCourse.open();

            given(courseRepository.findByIdWithLock(courseId)).willReturn(Optional.of(unlimitedCourse));
            given(enrollmentRepository.existsByCourseIdAndUserIdAndStatusIn(courseId, userId, ACTIVE_STATUSES)).willReturn(false);
            given(enrollmentRepository.save(any(Enrollment.class))).willReturn(enrollment);

            // [When]
            EnrollmentResponse result = enrollmentService.enroll(courseId, userId);

            // [Then]
            assertThat(result).isNotNull();
            verify(enrollmentRepository, never()).countByCourseIdAndStatusIn(any(), any());
            verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
        }

        @Test
        @DisplayName("강의를 찾지 못한 경우 예외 발생")
        void enroll_fail_courseNotFound() {
            // [Given]
            given(courseRepository.findByIdWithLock(courseId)).willReturn(Optional.empty());

            // [When & Then]
            assertThatThrownBy(() -> enrollmentService.enroll(courseId, userId))
                    .isInstanceOf(CourseNotFoundException.class)
                    .hasMessage("해당하는 강의를 찾을 수 없습니다.");

            verify(enrollmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("강의 상태가 OPEN이 아닌 경우 예외 발생")
        void enroll_fail_courseNotOpen() {
            // [Given] course는 기본 DRAFT 상태
            given(courseRepository.findByIdWithLock(courseId)).willReturn(Optional.of(course));

            // [When & Then]
            assertThatThrownBy(() -> enrollmentService.enroll(courseId, userId))
                    .isInstanceOf(CourseNotOpenException.class)
                    .hasMessage("오픈되지 않은 강의입니다.");

            verify(enrollmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 PENDING 또는 CONFIRMED 상태의 신청이 존재하면 예외 발생")
        void enroll_fail_alreadyEnrolled() {
            // [Given]
            course.open();
            given(courseRepository.findByIdWithLock(courseId)).willReturn(Optional.of(course));
            given(enrollmentRepository.existsByCourseIdAndUserIdAndStatusIn(courseId, userId, ACTIVE_STATUSES)).willReturn(true);

            // [When & Then]
            assertThatThrownBy(() -> enrollmentService.enroll(courseId, userId))
                    .isInstanceOf(AlreadyEnrolledException.class);

            verify(enrollmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("정원이 초과된 경우 예외 발생")
        void enroll_fail_capacityExceeded() {
            // [Given] maxCapacity = 30, currentCount = 30
            course.open();
            given(courseRepository.findByIdWithLock(courseId)).willReturn(Optional.of(course));
            given(enrollmentRepository.existsByCourseIdAndUserIdAndStatusIn(courseId, userId, ACTIVE_STATUSES)).willReturn(false);
            given(enrollmentRepository.countByCourseIdAndStatusIn(courseId, ACTIVE_STATUSES)).willReturn(30);

            // [When & Then]
            assertThatThrownBy(() -> enrollmentService.enroll(courseId, userId))
                    .isInstanceOf(EnrollmentCapacityExceededException.class)
                    .hasMessage("수강 정원이 초과되었습니다.");

            verify(enrollmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("confirm() 기능 테스트")
    class ConfirmTest {

        @Test
        @DisplayName("결제 확정 성공 - PENDING에서 CONFIRMED로 상태 변경")
        void confirm_success() {
            // [Given]
            given(enrollmentRepository.findByIdAndUserId(enrollmentId, userId)).willReturn(Optional.of(enrollment));

            // [When]
            enrollmentService.confirm(enrollmentId, userId);

            // [Then]
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
            assertThat(enrollment.getConfirmedAt()).isNotNull();

            verify(enrollmentRepository, times(1)).findByIdAndUserId(enrollmentId, userId);
        }

        @Test
        @DisplayName("수강 신청 내역을 찾지 못한 경우 예외 발생")
        void confirm_fail_enrollmentNotFound() {
            // [Given]
            given(enrollmentRepository.findByIdAndUserId(enrollmentId, userId)).willReturn(Optional.empty());

            // [When & Then]
            assertThatThrownBy(() -> enrollmentService.confirm(enrollmentId, userId))
                    .isInstanceOf(EnrollmentNotFoundException.class)
                    .hasMessage("수강 신청 내역을 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("cancel() 기능 테스트")
    class CancelTest {

        @Test
        @DisplayName("CONFIRMED 취소 시 대기열 첫 번째 사람 PENDING 승격")
        void cancel_confirmed_promotesWaiting() {
            // [Given]
            Enrollment waitingUser = Enrollment.reserve(userId + 1, courseId, 1);

            given(enrollmentRepository.findByIdAndUserId(2L, userId))
                    .willReturn(Optional.of(confirmedEnrollment));
            given(courseRepository.findByIdWithLock(courseId))
                    .willReturn(Optional.of(course));
            given(enrollmentRepository.findFirstByCourseIdAndStatusOrderByWaitlistPositionAsc(
                    courseId, EnrollmentStatus.WAITING))
                    .willReturn(Optional.of(waitingUser));

            // [When]
            enrollmentService.cancel(2L, userId);

            // [Then]
            assertThat(confirmedEnrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
            assertThat(waitingUser.getStatus()).isEqualTo(EnrollmentStatus.PENDING);

            verify(enrollmentRepository, times(1))
                    .findFirstByCourseIdAndStatusOrderByWaitlistPositionAsc(courseId, EnrollmentStatus.WAITING);
        }

        @Test
        @DisplayName("PENDING 취소 시 대기열 첫 번째 사람 PENDING 승격")
        void cancel_pending_promotesWaiting() {
            // [Given]
            Enrollment waitingUser = Enrollment.reserve(userId + 1, courseId, 1);

            given(enrollmentRepository.findByIdAndUserId(enrollmentId, userId))
                    .willReturn(Optional.of(pendingEnrollment));
            given(courseRepository.findByIdWithLock(courseId))
                    .willReturn(Optional.of(course));
            given(enrollmentRepository.findFirstByCourseIdAndStatusOrderByWaitlistPositionAsc(
                    courseId, EnrollmentStatus.WAITING))
                    .willReturn(Optional.of(waitingUser));

            // [When]
            enrollmentService.cancel(enrollmentId, userId);

            // [Then]
            assertThat(pendingEnrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
            assertThat(waitingUser.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
        }

        @Test
        @DisplayName("WAITING 취소 시 자리가 생기지 않으므로 대기열 승격 없음")
        void cancel_waiting_noPromotion() {
            // [Given]
            given(enrollmentRepository.findByIdAndUserId(3L, userId))
                    .willReturn(Optional.of(waitingEnrollment));
            given(courseRepository.findByIdWithLock(courseId))
                    .willReturn(Optional.of(course));

            // [When]
            enrollmentService.cancel(3L, userId);

            // [Then]
            assertThat(waitingEnrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
            verify(enrollmentRepository, never())
                    .findFirstByCourseIdAndStatusOrderByWaitlistPositionAsc(any(), any());
        }

        @Test
        @DisplayName("취소 후 대기열이 없는 경우 승격 없음")
        void cancel_confirmed_noWaiting() {
            // [Given]
            given(enrollmentRepository.findByIdAndUserId(2L, userId))
                    .willReturn(Optional.of(confirmedEnrollment));
            given(courseRepository.findByIdWithLock(courseId))
                    .willReturn(Optional.of(course));
            given(enrollmentRepository.findFirstByCourseIdAndStatusOrderByWaitlistPositionAsc(
                    courseId, EnrollmentStatus.WAITING))
                    .willReturn(Optional.empty());

            // [When]
            enrollmentService.cancel(2L, userId);

            // [Then]
            assertThat(confirmedEnrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
            verify(enrollmentRepository, times(1))
                    .findFirstByCourseIdAndStatusOrderByWaitlistPositionAsc(courseId, EnrollmentStatus.WAITING);
        }

        @Test
        @DisplayName("수강신청을 찾지 못한 경우 예외 발생")
        void cancel_fail_enrollmentNotFound() {
            // [Given]
            given(enrollmentRepository.findByIdAndUserId(enrollmentId, userId))
                    .willReturn(Optional.empty());

            // [When & Then]
            assertThatThrownBy(() -> enrollmentService.cancel(enrollmentId, userId))
                    .isInstanceOf(EnrollmentNotFoundException.class);

            verify(courseRepository, never()).findByIdWithLock(any());
        }
    }

    @Nested
    @DisplayName("getEnrollmentsByUserId() 기능 테스트")
    class GetEnrollmentsByUserIdTest {

        @Test
        @DisplayName("사용자 수강 목록 조회 성공")
        void getEnrollmentsByUserId_success() {
            // [Given]
            Pageable pageable = PageRequest.of(0, 20);
            Page<Enrollment> enrollmentPage = new PageImpl<>(List.of(enrollment));
            given(enrollmentRepository.findByUserId(userId, pageable)).willReturn(enrollmentPage);

            // [When]
            Page<EnrollmentResponse> result = enrollmentService.getEnrollmentsByUserId(userId, pageable);

            // [Then]
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).userId()).isEqualTo(userId);
            assertThat(result.getContent().get(0).courseId()).isEqualTo(courseId);

            verify(enrollmentRepository, times(1)).findByUserId(userId, pageable);
        }
    }

    @Nested
    @DisplayName("getCourseEnrollments() 기능 테스트")
    class GetCourseEnrollmentsTest {

        @Test
        @DisplayName("강의별 수강생 목록 조회 성공")
        void getCourseEnrollments_success() {
            // [Given]
            Pageable pageable = PageRequest.of(0, 20);
            Page<Enrollment> enrollmentPage = new PageImpl<>(List.of(enrollment));
            given(enrollmentRepository.findByCourseId(courseId, pageable)).willReturn(enrollmentPage);

            // [When]
            Page<EnrollmentResponse> result = enrollmentService.getCourseEnrollments(courseId, pageable);

            // [Then]
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).courseId()).isEqualTo(courseId);

            verify(enrollmentRepository, times(1)).findByCourseId(courseId, pageable);
        }
    }

    @Nested
    @DisplayName("reserve() 기능 테스트")
    class ReserveTest {

        @Test
        @DisplayName("자리가 있는 경우 PENDING으로 생성 성공")
        void reserve_success_pending() {
            // [Given]
            given(courseRepository.findByIdWithLock(courseId))
                    .willReturn(Optional.of(course));
            given(enrollmentRepository.existsByCourseIdAndUserIdAndStatusIn(
                    courseId, userId,
                    List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED, EnrollmentStatus.WAITING)))
                    .willReturn(false);
            given(enrollmentRepository.countByCourseIdAndStatusIn(
                    courseId, List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED)))
                    .willReturn(29); // 정원 30명, 29명 신청 → 자리 있음

            // [When]
            EnrollmentWithWaitCountResponse result = enrollmentService.reserve(courseId, userId);

            // [Then]
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(EnrollmentStatus.PENDING);
            assertThat(result.order()).isEqualTo(0L);

            verify(enrollmentRepository, never()).findMaxWaitlistPositionByCourseId(any());
            verify(enrollmentRepository, never()).countUserWaitingOrder(any(), any(), any());
            verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
        }

        @Test
        @DisplayName("정원이 꽉 찬 경우 첫 번째 대기자로 WAITING 생성 성공")
        void reserve_success_waiting_first() {
            // [Given]
            given(courseRepository.findByIdWithLock(courseId))
                    .willReturn(Optional.of(course));
            given(enrollmentRepository.existsByCourseIdAndUserIdAndStatusIn(
                    courseId, userId,
                    List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED, EnrollmentStatus.WAITING)))
                    .willReturn(false);
            given(enrollmentRepository.countByCourseIdAndStatusIn(
                    courseId, List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED)))
                    .willReturn(30); // 정원 30명, 30명 → 꽉 참
            given(enrollmentRepository.findMaxWaitlistPositionByCourseId(courseId))
                    .willReturn(Optional.empty()); // 기존 대기자 없음
            given(enrollmentRepository.countUserWaitingOrder(courseId, 1, EnrollmentStatus.WAITING))
                    .willReturn(0L);

            // [When]
            EnrollmentWithWaitCountResponse result = enrollmentService.reserve(courseId, userId);

            // [Then]
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(EnrollmentStatus.WAITING);
            assertThat(result.order()).isEqualTo(1L);

            verify(enrollmentRepository, times(1)).findMaxWaitlistPositionByCourseId(courseId);
            verify(enrollmentRepository, times(1)).countUserWaitingOrder(courseId, 1, EnrollmentStatus.WAITING);
            verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
        }

        @Test
        @DisplayName("정원이 꽉 찬 경우 기존 대기열에 추가 성공")
        void reserve_success_waiting_with_existing() {
            // [Given]
            given(courseRepository.findByIdWithLock(courseId))
                    .willReturn(Optional.of(course));
            given(enrollmentRepository.existsByCourseIdAndUserIdAndStatusIn(
                    courseId, userId,
                    List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED, EnrollmentStatus.WAITING)))
                    .willReturn(false);
            given(enrollmentRepository.countByCourseIdAndStatusIn(
                    courseId, List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED)))
                    .willReturn(30); // 꽉 참
            given(enrollmentRepository.findMaxWaitlistPositionByCourseId(courseId))
                    .willReturn(Optional.of(3)); // 기존 대기자 3명
            given(enrollmentRepository.countUserWaitingOrder(courseId, 4, EnrollmentStatus.WAITING))
                    .willReturn(3L); // 내 앞에 3명

            // [When]
            EnrollmentWithWaitCountResponse result = enrollmentService.reserve(courseId, userId);

            // [Then]
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(EnrollmentStatus.WAITING);
            assertThat(result.order()).isEqualTo(4L);

            verify(enrollmentRepository, times(1)).findMaxWaitlistPositionByCourseId(courseId);
            verify(enrollmentRepository, times(1)).countUserWaitingOrder(courseId, 4, EnrollmentStatus.WAITING);
            verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
        }

        @Test
        @DisplayName("강의를 찾지 못한 경우 예외 발생")
        void reserve_fail_courseNotFound() {
            // [Given]
            given(courseRepository.findByIdWithLock(courseId))
                    .willReturn(Optional.empty());

            // [When & Then]
            assertThatThrownBy(() -> enrollmentService.reserve(courseId, userId))
                    .isInstanceOf(CourseNotFoundException.class)
                    .hasMessage("해당하는 강의를 찾을 수 없습니다.");

            verify(enrollmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 신청한 강의에 중복 신청 시 예외 발생")
        void reserve_fail_alreadyEnrolled() {
            // [Given]
            given(courseRepository.findByIdWithLock(courseId))
                    .willReturn(Optional.of(course));
            given(enrollmentRepository.existsByCourseIdAndUserIdAndStatusIn(
                    courseId, userId,
                    List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED, EnrollmentStatus.WAITING)))
                    .willReturn(true);

            // [When & Then]
            assertThatThrownBy(() -> enrollmentService.reserve(courseId, userId))
                    .isInstanceOf(AlreadyEnrolledException.class);

            verify(enrollmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getEnrollmentDetail() 기능 테스트")
    class GetEnrollmentDetailTest {

        @Test
        @DisplayName("수강신청 상세 조회 성공")
        void getEnrollmentDetail_success() {
            // [Given]
            given(enrollmentRepository.findById(enrollmentId))
                    .willReturn(Optional.of(pendingEnrollment));

            // [When]
            EnrollmentResponse result = enrollmentService.getEnrollmentDetail(enrollmentId);

            // [Then]
            assertThat(result).isNotNull();
            verify(enrollmentRepository, times(1)).findById(enrollmentId);
        }

        @Test
        @DisplayName("수강신청을 찾지 못한 경우 예외 발생")
        void getEnrollmentDetail_fail_notFound() {
            // [Given]
            given(enrollmentRepository.findById(enrollmentId))
                    .willReturn(Optional.empty());

            // [When & Then]
            assertThatThrownBy(() -> enrollmentService.getEnrollmentDetail(enrollmentId))
                    .isInstanceOf(EnrollmentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getMyReserveDetail() 기능 테스트")
    class GetMyReserveDetailTest {

        @Test
        @DisplayName("WAITING 상태 대기열 상세 조회 성공")
        void getMyReserveDetail_success() {
            // [Given]
            Enrollment waiting = Enrollment.reserve(userId, courseId, 3);
            ReflectionTestUtils.setField(waiting, "id", enrollmentId);

            given(enrollmentRepository.findById(enrollmentId))
                    .willReturn(Optional.of(waiting));
            given(enrollmentRepository.countUserWaitingOrder(courseId, 3, EnrollmentStatus.WAITING))
                    .willReturn(2L); // 앞에 2명 → 순번 3번

            // [When]
            EnrollmentWithWaitCountResponse result = enrollmentService.getMyReserveDetail(enrollmentId);

            // [Then]
            assertThat(result).isNotNull();
            assertThat(result.order()).isEqualTo(3L);

            verify(enrollmentRepository, times(1))
                    .countUserWaitingOrder(courseId, 3, EnrollmentStatus.WAITING);
        }

        @Test
        @DisplayName("수강신청을 찾지 못한 경우 예외 발생")
        void getMyReserveDetail_fail_notFound() {
            // [Given]
            given(enrollmentRepository.findById(enrollmentId))
                    .willReturn(Optional.empty());

            // [When & Then]
            assertThatThrownBy(() -> enrollmentService.getMyReserveDetail(enrollmentId))
                    .isInstanceOf(EnrollmentNotFoundException.class);

            verify(enrollmentRepository, never()).countUserWaitingOrder(any(), any(), any());
        }

        @Test
        @DisplayName("WAITING 상태가 아닌 경우 예외 발생")
        void getMyReserveDetail_fail_notWaiting() {
            // [Given]
            given(enrollmentRepository.findById(enrollmentId))
                    .willReturn(Optional.of(pendingEnrollment));

            // [When & Then]
            assertThatThrownBy(() -> enrollmentService.getMyReserveDetail(enrollmentId))
                    .isInstanceOf(EnrollmentStatusNotWaiting.class);

            verify(enrollmentRepository, never()).countUserWaitingOrder(any(), any(), any());
        }
    }
}