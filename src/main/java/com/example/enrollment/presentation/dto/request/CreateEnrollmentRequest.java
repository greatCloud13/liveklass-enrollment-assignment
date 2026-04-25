package com.example.enrollment.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateEnrollmentRequest(
        @NotNull(message = "강의 ID는 필수입니다.")
        Long courseId
) {}