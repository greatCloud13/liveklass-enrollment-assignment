package com.example.enrollment.domain.enrollment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.Optional;


public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

//  PENDING + CONFIRMED 계산
    int countByCourseIdAndStatusIn(Long courseId, List<EnrollmentStatus> statuses);

//  중복 신청 검증
    boolean existsByCourseIdAndUserIdAndStatusIn(Long courseId, Long userId, List<EnrollmentStatus> statuses);

    Optional<Enrollment> findByIdAndUserId(Long courseId, Long userId);

    Page<Enrollment> findByUserId(Long userId, Pageable pageable);

    Page<Enrollment> findByCourseId(Long courseId, Pageable pageable);

}
