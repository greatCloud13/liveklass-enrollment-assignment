package com.example.enrollment.presentation.dto.response;

import com.example.enrollment.domain.enrollment.Enrollment;
import com.example.enrollment.domain.enrollment.EnrollmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "수강 신청 대기 응답")
public record EnrollmentWithWaitCountResponse(

        @Schema(description = "신청 ID", example = "1")
        Long id,

        @Schema(description = "신청자 ID", example = "1")
        Long userId,

        @Schema(description = "강의 ID", example = "1")
        Long courseId,

        @Schema(description = "요청 상태", example = "PENDING")
        EnrollmentStatus status,

        @Schema(description = "대기 신청 일시", example = "2025-05-02")
        LocalDateTime createdAt,

        @Schema(description = "현재 대기 순번", example = "1")
        Long order
){
    public static EnrollmentWithWaitCountResponse from(Enrollment enrollment,
                                                       Long order ){
        return new EnrollmentWithWaitCountResponse(
                enrollment.getId(),
                enrollment.getUserId(),
                enrollment.getCourseId(),
                enrollment.getStatus(),
                enrollment.getCreatedAt(),
                order
        );

    }

}
