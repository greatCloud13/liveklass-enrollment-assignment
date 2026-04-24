package com.example.enrollment.presentation.dto.response;

import com.example.enrollment.domain.course.Course;
import com.example.enrollment.domain.course.CourseStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "강의 목록 응답")
public record CourseResponse(

        @Schema(description = "강의 ID", example = "1")
        Long id,

        @Schema(description = "강의 제목", example = "Spring Boot 완전 정복")
        String title,

        @Schema(description = "수강료", example = "50000")
        BigDecimal price,

        @Schema(description = "최대 수강 인원 (null = 무제한)", example = "30")
        Integer maxCapacity,

        @Schema(description = "강의 상태", example = "OPEN")
        CourseStatus status,

        @Schema(description = "강의 시작일", example = "2025-05-01")
        LocalDate startDate,

        @Schema(description = "강의 종료일", example = "2025-07-31")
        LocalDate endDate,

        @Schema(description = "강사 ID", example = "1")
        Long instructorId

) {
    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getPrice(),
                course.getMaxCapacity(),
                course.getStatus(),
                course.getStartDate(),
                course.getEndDate(),
                course.getInstructorId()
        );
    }
}