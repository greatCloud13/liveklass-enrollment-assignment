package com.example.enrollment.application;

import com.example.enrollment.common.exception.CourseNotFoundException;
import com.example.enrollment.common.exception.InvalidCoursePeriodException;
import com.example.enrollment.domain.course.Course;
import com.example.enrollment.domain.course.CourseRepository;
import com.example.enrollment.domain.course.CourseStatus;
import com.example.enrollment.presentation.dto.request.CreateCourseRequest;
import com.example.enrollment.presentation.dto.response.CourseDetailResponse;
import com.example.enrollment.presentation.dto.response.CourseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;

    /**
     * 강의를 생성합니다. 초기 상태는 DRAFT이며 OPEN 전환 후 수강 신청이 가능합니다.
     */
    @Transactional
    public CourseDetailResponse createCourse(Long instructorId, CreateCourseRequest request){

//      #todo 인증 시스템 개발시 크리에이터 검증 로직 필요

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new InvalidCoursePeriodException();
        }

        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .maxCapacity(request.getMaxCapacity())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .instructorId(instructorId)
                .build();

        courseRepository.save(course);

        return CourseDetailResponse.from(course, 0);
    }

    @Transactional
    public CourseDetailResponse openCourse(Long instructorId, Long courseId){

        Course course = courseRepository.findById(courseId)
                .orElseThrow(CourseNotFoundException :: new);
        course.validateOwnership(instructorId);

        course.open();

        return CourseDetailResponse.from(course, 0);
    }

    @Transactional
    public CourseDetailResponse closeCourse(Long instructorId, Long courseId){

        Course course = courseRepository.findById(courseId)
                .orElseThrow(CourseNotFoundException :: new);
        course.validateOwnership(instructorId);

        course.close();

//      #todo 수강신청 인원 계산 기능 구현이 되어 있지 않으므로 하드 코딩 구현 완료시 수정 필요
        int currentEnrollmentCount = 10;

        return CourseDetailResponse.from(course, currentEnrollmentCount);
    }

    /**
     * 상태를 기준으로 강의를 조회합니다. CourseStatus가 Null일 경우 전체 조회
     */
    public Page<CourseResponse> getCoursePage(Pageable pageable, CourseStatus status){
        Page<Course> coursePage = (status != null) ?
                courseRepository.findByStatus(pageable, status)
                : courseRepository.findAll(pageable);

        return coursePage.map(CourseResponse :: from);
    }

    public CourseDetailResponse getCourseDetail(Long courseId){

        Course course = courseRepository.findById(courseId)
                .orElseThrow(CourseNotFoundException :: new);

//      #todo 수강신청 인원 계산 기능 구현이 되어 있지 않으므로 하드 코딩 구현 완료시 수정 필요
        int currentEnrollmentCount = 10;

        return CourseDetailResponse.from(course, currentEnrollmentCount);
    }
}
