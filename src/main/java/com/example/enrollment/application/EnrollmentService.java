package com.example.enrollment.application;

import com.example.enrollment.common.exception.AlreadyEnrolledException;
import com.example.enrollment.common.exception.CourseNotFoundException;
import com.example.enrollment.common.exception.EnrollmentNotFoundException;
import com.example.enrollment.domain.course.Course;
import com.example.enrollment.domain.course.CourseRepository;
import com.example.enrollment.domain.enrollment.Enrollment;
import com.example.enrollment.domain.enrollment.EnrollmentRepository;
import com.example.enrollment.domain.enrollment.EnrollmentStatus;
import com.example.enrollment.presentation.dto.response.EnrollmentResponse;
import com.example.enrollment.presentation.dto.response.EnrollmentWithWaitCountResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;

    /**
     * PENDING 상태로 신청 정보를 생성합니다.
     */
    @Transactional
    public EnrollmentResponse enroll(Long courseId, Long userId){

        log.info("수강 신청을 생성합니다. 신청 강의 ID: {}, 요청자 ID: {}", courseId, userId);

        Course course = courseRepository.findByIdWithLock(courseId)
                .orElseThrow(CourseNotFoundException :: new);

        course.validateEnrollable();

        if(enrollmentRepository.existsByCourseIdAndUserIdAndStatusIn(
                course.getId(), userId, List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED))){
            throw new AlreadyEnrolledException();
        }

        if(course.getMaxCapacity() != null){
            int currentCount = enrollmentRepository.countByCourseIdAndStatusIn(
                    courseId, List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED));

            course.validateCapacity(currentCount);
        }

        Enrollment enrollment = Enrollment.builder()
                .courseId(courseId)
                .userId(userId)
                .build();

        enrollmentRepository.save(enrollment);

        return EnrollmentResponse.from(enrollment);
    }

    @Transactional
    public void confirm(Long enrollmentId, Long userId) {
        Enrollment enrollment = enrollmentRepository.findByIdAndUserId(enrollmentId, userId)
                .orElseThrow(EnrollmentNotFoundException :: new);

        enrollment.confirm();
    }

    @Transactional
    public void cancel(Long enrollmentId, Long userId) {
        Enrollment enrollment = enrollmentRepository.findByIdAndUserId(enrollmentId, userId)
                .orElseThrow(EnrollmentNotFoundException :: new);

        enrollment.cancel();
    }

    public Page<EnrollmentResponse> getEnrollmentsByUserId(Long userId, Pageable pageable) {
        return enrollmentRepository.findByUserId(userId, pageable)
                .map(EnrollmentResponse::from);
    }

    public Page<EnrollmentResponse> getCourseEnrollments(Long courseId, Pageable pageable) {
        return enrollmentRepository.findByCourseId(courseId, pageable)
                .map(EnrollmentResponse::from);
    }

    /**
     * WAITING 상태로 신청 정보를 생성합니다.
     * (빈자리가 있을 경우 PENDING으로 생성합니다.)
     */
    @Transactional
    public EnrollmentWithWaitCountResponse reserve(Long courseId, Long userId){

        Course course = courseRepository.findByIdWithLock(courseId)
                .orElseThrow(CourseNotFoundException :: new);

        if(enrollmentRepository.existsByCourseIdAndUserIdAndStatusIn(
                course.getId(), userId,
                List.of(EnrollmentStatus.PENDING,
                        EnrollmentStatus.CONFIRMED,
                        EnrollmentStatus.WAITING))){
            throw new AlreadyEnrolledException();
        }


        boolean isFull = enrollmentRepository.countByCourseIdAndStatusIn(courseId, List.of(
                EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED))
                >= course.getMaxCapacity();

        int waitlistCount = isFull
                ? enrollmentRepository.findMaxWaitlistPositionByCourseId(courseId).orElse(0)
                : 0;

        Enrollment enrollment = isFull
                ? Enrollment.reserve(userId, courseId, waitlistCount + 1)
                : Enrollment.builder().courseId(courseId).userId(userId).build();

        Long order = isFull
                ? enrollmentRepository.countUserWaitingOrder(courseId, waitlistCount + 1, EnrollmentStatus.WAITING) + 1
                : 0L;

        enrollmentRepository.save(enrollment);

        return EnrollmentWithWaitCountResponse.from(enrollment, order);
    }
}
