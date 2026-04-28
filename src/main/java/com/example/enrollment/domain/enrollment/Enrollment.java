package com.example.enrollment.domain.enrollment;

import com.example.enrollment.common.exception.EnrollmentCancelNotAllowedException;
import com.example.enrollment.common.exception.EnrollmentStatusNotWaiting;
import com.example.enrollment.common.exception.InvalidStatusTransitionException;
import com.example.enrollment.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name="enrollment",
        indexes = {
                @Index(name = "idx_enrollment_course_id", columnList = "course_id"),
                @Index(name = "idx_enrollment_user_id", columnList = "user_id")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // 외부 직접 생성 방지
public class Enrollment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EnrollmentStatus status;

    @Column
    private LocalDateTime confirmedAt;

    @Column
    private LocalDateTime cancelledAt;

    @Column
    private Integer waitlistPosition;


    @Builder
    public Enrollment(Long userId, Long courseId) {
        this.userId = userId;
        this.courseId = courseId;
        this.status = EnrollmentStatus.PENDING;
    }

    public static Enrollment reserve(Long userId, Long courseId, Integer waitlistPosition){
        Enrollment enrollment = new Enrollment();
        enrollment.userId = userId;
        enrollment.courseId = courseId;
        enrollment.status = EnrollmentStatus.WAITING;
        enrollment.waitlistPosition = waitlistPosition;
        return enrollment;
    }

    public void confirm() {
        if (this.status != EnrollmentStatus.PENDING) {
            throw new InvalidStatusTransitionException(this.status.name(), EnrollmentStatus.CONFIRMED.name());
        }
        this.status = EnrollmentStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    public void toPending(){
        if (this.status != EnrollmentStatus.WAITING) {
            throw new InvalidStatusTransitionException(this.status.name(), EnrollmentStatus.PENDING.name());
        }
        this.status = EnrollmentStatus.PENDING;
    }

    public void cancel() {
        switch (this.status) {
            case PENDING -> { /*결제가 진행되지 않았음으로 별도 검증없이 취소 상태 변환*/ }
            case CONFIRMED -> {
                if (!isCancellable()) {
                    throw new EnrollmentCancelNotAllowedException();
                }
            }
            case CANCELLED -> throw new InvalidStatusTransitionException(
                    this.status.name(), EnrollmentStatus.CANCELLED.name());
        }
        this.status = EnrollmentStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    public boolean isCancellable() {
        // 결제 확정 후 7일 후 당일을 포함하여 취소 가능
        return confirmedAt != null &&
                !LocalDateTime.now().isAfter(confirmedAt.plusDays(7));
    }

    public void validateWaiting(){
        if(this.status != EnrollmentStatus.WAITING){
            throw new EnrollmentStatusNotWaiting(this.status);
        }
    }

}
