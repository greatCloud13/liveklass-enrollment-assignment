package com.example.enrollment.presentation.dto;

import com.example.enrollment.application.CourseService;
import com.example.enrollment.common.exception.*;
import com.example.enrollment.domain.course.CourseStatus;
import com.example.enrollment.presentation.dto.request.CreateCourseRequest;
import com.example.enrollment.presentation.dto.response.CourseDetailResponse;
import com.example.enrollment.presentation.dto.response.CourseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Tag(name = "Course", description = "강의 관리 API")
@RestController
@RequestMapping("/api/v1/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @Operation(
            summary = "강의 생성",
            description = "새 강의를 DRAFT 상태로 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공 — Location 헤더에 리소스 URI 포함"),
            @ApiResponse(responseCode = "400", description = "요청 데이터 유효성 검증 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "수강 기간 유효성 검증 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))

    })
    @PostMapping
    public ResponseEntity<CourseDetailResponse> createCourse(@RequestHeader("X-User-Id") Long instructorId,
                                                             @Valid @RequestBody CreateCourseRequest request){

        CourseDetailResponse result = courseService.createCourse(instructorId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(result.id())
                .toUri();

        return ResponseEntity.created(location).body(result);
    }


    @Operation(
            summary = "강의 목록 조회",
            description = "상태별 강의 목록을 페이지 단위로 조회합니다. status가 Null일 경우 전체 조회"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 status 값", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<Page<CourseResponse>> getCoursePage(@ParameterObject Pageable pageable,
                                                              @RequestParam(required = false) CourseStatus status){

        Page<CourseResponse> result = courseService.getCoursePage(pageable, status);

        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "강의 상세 조회",
            description = "강의 ID로 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "강의를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{courseId}")
    public ResponseEntity<CourseDetailResponse> getCourseDetail(@PathVariable Long courseId){

        CourseDetailResponse result = courseService.getCourseDetail(courseId);

        return ResponseEntity.ok(result);
    }


    @Operation(
            summary = "강의 모집 시작",
            description = "DRAFT 상태의 강의를 OPEN으로 전환합니다. 강의 소유자만 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "모집 상태 변환 성공"),
            @ApiResponse(responseCode = "400", description = "DRAFT 상태가 아니기 때문에 전환 불가", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "강의 소유자가 아님", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "강의를 찾을 수 없음,", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{courseId}/open")
    public ResponseEntity<CourseDetailResponse> openCourse(@RequestHeader("X-User-Id") Long instructorId,
                                                           @PathVariable Long courseId){

        CourseDetailResponse result = courseService.openCourse(instructorId, courseId);

        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "강의 모집 마감",
            description = "OPEN 상태의 강의를 CLOSE로 전환합니다. 되돌릴 수 없습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "모집 마감 상태 변환 성공"),
            @ApiResponse(responseCode = "400", description = "OPEN 상태가 아닌 경우 변환 불가", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "강의 소유자가 아님", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "강의를 찾을 수 없음,", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{courseId}/close")
    public ResponseEntity<CourseDetailResponse> closeCourse(@RequestHeader("X-User-Id") Long instructorId,
                                                            @PathVariable Long courseId) {

        CourseDetailResponse result = courseService.closeCourse(instructorId, courseId);

        return ResponseEntity.ok(result);
    }
}
