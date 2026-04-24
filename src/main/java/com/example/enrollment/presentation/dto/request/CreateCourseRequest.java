package com.example.enrollment.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "강의 생성 요청")
public class CreateCourseRequest {

    @Schema(description = "강의 제목", example = "라이브 클래스 과제 테스트 강의", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "강의 제목은 필수입니다.")
    private String title;

    @Schema(description = "강의 설명", example = "라이브 클래스의 테스트 과제 입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "강의 설명은 필수입니다.")
    private String description;

    @Schema(description = "수강료", example = "50000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "수강료는 필수입니다.")
    @PositiveOrZero(message = "수강료는 0 이상이어야 합니다.")
    private BigDecimal price;

    @Schema(description = "최대 수강 인원 (null = 무제한)", example = "30")
    @Min(value = 1, message = "최소 인원은 1명 이상이어야 합니다.")
    private Integer maxCapacity;

    @Schema(description = "강의 시작일", example = "2025-05-01", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "시작일은 필수입니다.")
    private LocalDate startDate;

    @Schema(description = "강의 종료일", example = "2025-07-31", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "종료일은 필수입니다.")
    private LocalDate endDate;
}
