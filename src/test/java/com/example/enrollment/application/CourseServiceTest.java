package com.example.enrollment.application;

import com.example.enrollment.common.exception.CourseNotFoundException;
import com.example.enrollment.common.exception.InvalidAccessException;
import com.example.enrollment.common.exception.InvalidCoursePeriodException;
import com.example.enrollment.common.exception.InvalidStatusTransitionException;
import com.example.enrollment.domain.course.Course;
import com.example.enrollment.domain.course.CourseRepository;
import com.example.enrollment.domain.course.CourseStatus;
import com.example.enrollment.presentation.dto.request.CreateCourseRequest;
import com.example.enrollment.presentation.dto.response.CourseDetailResponse;
import com.example.enrollment.presentation.dto.response.CourseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
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
public class CourseServiceTest {

    @InjectMocks
    private CourseService courseService;

    @Mock
    private CourseRepository courseRepository;

    private final Long instructorId = 1L;
    private final Long invalidInstructorId = 99L;
    private final Long courseId = 50L;
    private final Long emptyCourseId = 999L;

    private Course course;
    private CreateCourseRequest createCourseRequest;
    private CourseDetailResponse courseDetailResponse;
    private CourseResponse courseResponse;

    @BeforeEach
    void setUp() {
        course = Course.builder()
                .title("테스트 강의")
                .description("강의 설명")
                .price(BigDecimal.valueOf(50000))
                .startDate(LocalDate.now())
                .maxCapacity(500)
                .endDate(LocalDate.now().plusMonths(1))
                .instructorId(1L)
                .build();
        ReflectionTestUtils.setField(course, "id", courseId);

        createCourseRequest = CreateCourseRequest.builder()
                .title("테스트 강의")
                .description("강의 설명")
                .price(BigDecimal.valueOf(50000))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .maxCapacity(500)
                .build();

        courseDetailResponse = CourseDetailResponse.from(course, 0);
        courseResponse = CourseResponse.from(course);
    }

    @Nested
    @DisplayName("createCourse() 기능 테스트")
    class CreateCourseTest {

        @Test
        @DisplayName("강의 생성 성공")
        void createCourse_success(){
//          [Given]
            given(courseRepository.save(any(Course.class))).willReturn(course);
//          [When]
            CourseDetailResponse result = courseService.createCourse(instructorId, createCourseRequest);
//          [Then]
            assertThat(result).isNotNull();
            assertThat(result.title()).isEqualTo(createCourseRequest.getTitle());
            assertThat(result.description()).isEqualTo(createCourseRequest.getDescription());
            assertThat(result.price()).isEqualTo(createCourseRequest.getPrice());
            assertThat(result.maxCapacity()).isEqualTo(createCourseRequest.getMaxCapacity());
            assertThat(result.startDate()).isEqualTo(createCourseRequest.getStartDate());
            assertThat(result.endDate()).isEqualTo(createCourseRequest.getEndDate());
            assertThat(result.instructorId()).isEqualTo(instructorId);

            verify(courseRepository, times(1)).save(any(Course.class));
        }

        @Test
        @DisplayName("모집 종료일이 시작일 이전일 경우 예외 발생")
        void createCourse_fail_invalidPeriod(){
//          [Given]
            CreateCourseRequest invalidCourse = CreateCourseRequest.builder()
                    .startDate(LocalDate.of(2026,4,25))
                    .endDate(LocalDate.of(2026,4,24))
                    .build();
//          [When & Then]
            assertThatThrownBy(() -> courseService.createCourse(instructorId, invalidCourse))
                    .isInstanceOf(InvalidCoursePeriodException.class)
                    .hasMessage("종료일은 시작일 이후여야 합니다.");
        }
    }

    @Nested
    @DisplayName("openCourse() 기능 테스트")
    class openCourseTest{

        @Test
        @DisplayName("강의 오픈 성공")
        void openCourse_success(){
//          [Given]
            given(courseRepository.findById(50L)).willReturn(Optional.of(course));
//          [When]
            CourseDetailResponse result = courseService.openCourse(1L, 50L);
//          [Then]
            assertThat(result.status()).isEqualTo(CourseStatus.OPEN);
        }

        @Test
        @DisplayName("Open을 요청한 강의를 찾지 못했을 경우 예외 발생")
        void openCourse_fail_courseNotFound(){
//          [Given]
            given(courseRepository.findById(emptyCourseId)).willReturn(Optional.empty());
//          [When & Then]
            assertThatThrownBy(()->courseService.openCourse(1L, emptyCourseId))
                    .isInstanceOf(CourseNotFoundException.class)
                    .hasMessage("해당하는 강의를 찾을 수 없습니다.");

        }

        @Test
        @DisplayName("강의를 개설한 크리에이터 ID와 요청 ID가 일치 하지 않는 경우")
        void openCourse_fail_ownershipValidateFail(){
//          [Given]
            given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
//          [When & Then]
            assertThatThrownBy(()->courseService.openCourse(invalidInstructorId, courseId))
                    .isInstanceOf(InvalidAccessException.class)
                    .hasMessage("잘못된 접근입니다.");
        }

        @Test
        @DisplayName("Open을 요청한 강의의 상태가 Open일 경우 예외 발생")
        void openCourse_fail_courseStatusIsOpen(){
//          [Given]
            given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
            course.open();

//          [When & Then]
            assertThatThrownBy(()->courseService.openCourse(instructorId, courseId))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessage(String.format("상태를 변경할 수 없습니다. %s -> %s", course.getStatus().toString(), CourseStatus.OPEN));
        }

        @Test
        @DisplayName("Open을 요청한 강의의 상태가 Closed일 경우 예외 발생")
        void openCourse_fail_courseStatusIsClosed(){
//          [Given]
            given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
            course.open();
            course.close();

//          [When & Then]
            assertThatThrownBy(()->courseService.openCourse(instructorId, courseId))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessage(String.format("상태를 변경할 수 없습니다. %s -> %s", course.getStatus().toString(), CourseStatus.OPEN));
        }
    }

//  #todo 수강신청 인원 계산 기능 구현시 기능 추가 후 하드코딩된 파라미터 교체 및 테스트 수정 필요
    @Nested
    @DisplayName("closeCourse() 기능 테스트")
    class closeCourseTest{

        @Test
        @DisplayName("강의 모집 마감 성공")
        void closeCourse_success(){
//          [Given]
            course.open();
            given(courseRepository.findById(50L)).willReturn(Optional.of(course));
//          [When]
            CourseDetailResponse result = courseService.closeCourse(1L, 50L);
//          [Then]
            assertThat(result.status()).isEqualTo(CourseStatus.CLOSED);
        }

        @Test
        @DisplayName("Close을 요청한 강의를 찾지 못했을 경우 예외 발생")
        void closeCourse_fail_courseNotFound(){
//          [Given]
            given(courseRepository.findById(emptyCourseId)).willReturn(Optional.empty());
//          [When & Then]
            assertThatThrownBy(()->courseService.closeCourse(1L, emptyCourseId))
                    .isInstanceOf(CourseNotFoundException.class)
                    .hasMessage("해당하는 강의를 찾을 수 없습니다.");

        }

        @Test
        @DisplayName("강의를 개설한 크리에이터 ID와 요청 ID가 일치 하지 않는 경우")
        void closeCourse_fail_ownershipValidateFail(){
//          [Given]
            given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
//          [When & Then]
            assertThatThrownBy(()->courseService.closeCourse(invalidInstructorId, courseId))
                    .isInstanceOf(InvalidAccessException.class)
                    .hasMessage("잘못된 접근입니다.");
        }

        @Test
        @DisplayName("Close를 요청한 강의의 상태가 Draft일 경우 예외 발생")
        void openCourse_fail_courseStatusIsDraft(){
//          [Given]
            given(courseRepository.findById(courseId)).willReturn(Optional.of(course));

//          [When & Then]
            assertThatThrownBy(()->courseService.closeCourse(instructorId, courseId))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessage(String.format("상태를 변경할 수 없습니다. %s -> %s", course.getStatus().toString(), CourseStatus.CLOSED));
        }

        @Test
        @DisplayName("Close를 요청한 강의의 상태가 Closed일 경우 예외 발생")
        void openCourse_fail_courseStatusIsClosed(){
//          [Given]
            given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
            course.open();
            course.close();

//          [When & Then]
            assertThatThrownBy(()->courseService.closeCourse(instructorId, courseId))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessage(String.format("상태를 변경할 수 없습니다. %s -> %s", course.getStatus().toString(), CourseStatus.CLOSED));
        }


    }

    @Nested
    @DisplayName("getCoursePage() 기능 테스트")
    class getCoursePage{

        @Test
        @DisplayName("status가 null이 아닌 경우 상태 기준으로 조회")
        void getCoursePage_withStatus() {
            // [Given]
            Pageable pageable = PageRequest.of(0, 10);
            Page<Course> coursePage = new PageImpl<>(List.of(course));
            given(courseRepository.findByStatus(pageable, CourseStatus.OPEN)).willReturn(coursePage);

            // [When]
            Page<CourseResponse> result = courseService.getCoursePage(pageable, CourseStatus.OPEN);

            // [Then]
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).title()).isEqualTo(course.getTitle());

            verify(courseRepository, times(1)).findByStatus(pageable, CourseStatus.OPEN);
            verify(courseRepository, never()).findAll(pageable);
        }

        @Test
        @DisplayName("status가 null인 경우 전체 조회")
        void getCoursePage_withoutStatus() {
            // [Given]
            Pageable pageable = PageRequest.of(0, 10);
            Page<Course> coursePage = new PageImpl<>(List.of(course));
            given(courseRepository.findAll(pageable)).willReturn(coursePage);

            // [When]
            Page<CourseResponse> result = courseService.getCoursePage(pageable, null);

            // [Then]
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);

            verify(courseRepository, times(1)).findAll(pageable);
            verify(courseRepository, never()).findByStatus(any(), any());
        }

    }

//  #todo 수강신청 인원 계산 기능 구현시 기능 추가 후 하드코딩된 파라미터 교체 테스트 수정 필요
    @Nested
    @DisplayName("getCourseDetail() 기능 테스트")
    class GetCourseDetailTest {

        @Test
        @DisplayName("강의 상세 조회 성공")
        void getCourseDetail_success() {
            // [Given]
            given(courseRepository.findById(courseId)).willReturn(Optional.of(course));

            // [When]
            CourseDetailResponse result = courseService.getCourseDetail(courseId);

            // [Then]
            assertThat(result).isNotNull();
            assertThat(result.title()).isEqualTo(course.getTitle());
            assertThat(result.description()).isEqualTo(course.getDescription());
            assertThat(result.price()).isEqualTo(course.getPrice());
            assertThat(result.instructorId()).isEqualTo(course.getInstructorId());
            assertThat(result.currentEnrollmentCount()).isEqualTo(10); // 하드코딩된 값 검증

            verify(courseRepository, times(1)).findById(courseId);
        }

        @Test
        @DisplayName("강의 상세 조회 실패 - 강의를 찾지 못한 경우 예외 발생")
        void getCourseDetail_fail_courseNotFound() {
            // [Given]
            given(courseRepository.findById(emptyCourseId)).willReturn(Optional.empty());

            // [When & Then]
            assertThatThrownBy(() -> courseService.getCourseDetail(emptyCourseId))
                    .isInstanceOf(CourseNotFoundException.class)
                    .hasMessage("해당하는 강의를 찾을 수 없습니다.");

            verify(courseRepository, times(1)).findById(emptyCourseId);
        }
    }


}
