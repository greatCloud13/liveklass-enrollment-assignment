package com.example.enrollment.presentation.dto.response;

import com.example.enrollment.domain.enrollment.Enrollment;
import com.example.enrollment.domain.enrollment.EnrollmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "수강 신청 응답")
public record EnrollmentResponse(

        @Schema(description = "신청 ID", example = "1")
        Long id,

        @Schema(description = "신청자 ID", example = "1")
        Long userId,

        @Schema(description = "강의 ID", example = "1")
        Long courseId,

        @Schema(description = "요청 상태", example = "PENDING")
        EnrollmentStatus status,

        @Schema(description = "결제 완료 일자", example = "2025-05-01")
        LocalDateTime confirmedAt,

        @Schema(description = "취소 일자", example = "2025-05-01")
        LocalDateTime cancelledAt
){
    public static EnrollmentResponse from(Enrollment enrollment){
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getUserId(),
                enrollment.getCourseId(),
                enrollment.getStatus(),
                enrollment.getConfirmedAt(),
                enrollment.getCancelledAt()
        );

    }

}
