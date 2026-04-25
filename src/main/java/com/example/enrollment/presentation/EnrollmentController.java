package com.example.enrollment.presentation;

import com.example.enrollment.application.EnrollmentService;
import com.example.enrollment.common.exception.ErrorResponse;
import com.example.enrollment.presentation.dto.request.CreateEnrollmentRequest;
import com.example.enrollment.presentation.dto.response.EnrollmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/enrollments")
@SecurityRequirement(name = "X-User-Id")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @Operation(summary = "수강 신청", description = "PENDING 상태로 수강 신청을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공 — Location 헤더에 리소스 URI 포함"),
            @ApiResponse(responseCode = "400", description = "요청 데이터 유효성 검증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "강의를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "중복 신청 또는 정원 초과",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<Void> enroll(@RequestHeader("X-User-Id") Long userId,
                                       @Valid @RequestBody CreateEnrollmentRequest request){
        EnrollmentResponse result = enrollmentService.enroll(request.courseId(), userId);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(result.id())
                .toUri();

        return ResponseEntity.created(location).build();
    }


    @Operation(summary = "결제 확정", description = "PENDING 상태의 수강 신청을 CONFIRMED로 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "결제 확정 성공"),
            @ApiResponse(responseCode = "400", description = "상태 전이 불가",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "수강 신청 내역을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Void> confirm(@RequestHeader("X-User-Id") Long userId,
                                        @PathVariable Long id){
        enrollmentService.confirm(id, userId);
        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "수강 취소", description = "결제 확정 후 7일 이내 취소 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "취소 성공"),
            @ApiResponse(responseCode = "400", description = "취소 불가 (7일 초과 등)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "수강 신청 내역을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@RequestHeader("X-User-Id") Long userId,
                                       @PathVariable Long id){

        enrollmentService.cancel(id, userId);

        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "내 수강 목록", description = "본인의 수강 신청 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/me")
    public ResponseEntity<Page<EnrollmentResponse>> getMyEnrollments(@RequestHeader("X-User-Id") Long userId,
                                                                     @ParameterObject Pageable pageable){
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByUserId(userId, pageable));
    }


    @Operation(summary = "강의별 수강생 목록", description = "특정 강의의 수강 신청 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public ResponseEntity<Page<EnrollmentResponse>> getCourseEnrollments(@RequestHeader("X-User-Id") Long userId,
                                                                         @RequestParam Long courseId,
                                                                         @ParameterObject Pageable pageable){

        return ResponseEntity.ok(enrollmentService.getCourseEnrollments(courseId, pageable));
    }
}