package com.example.enrollment.domain.enrollment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


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

    Optional<Enrollment> findFirstByCourseIdAndStatusOrderByWaitlistPositionAsc(Long courseId, EnrollmentStatus status);

    Page<Enrollment> findByUserIdAndStatus(Long userId, EnrollmentStatus status, Pageable pageable);

    @Query("""
            SELECT MAX(e.waitlistPosition) FROM Enrollment e
            WHERE e.courseId = :courseId AND e.status = 'WAITING'
            """
    )
    Optional<Integer> findMaxWaitlistPositionByCourseId(@Param("courseId")Long courseId);

    @Query("""
            SELECT COUNT(e) FROM Enrollment e
            WHERE e.courseId = :courseId
            AND e.status = :status
            AND e.waitlistPosition < :myPosition
            """)
    Long countUserWaitingOrder(@Param("courseId")Long courseId, @Param("myPosition")Integer myPosition, @Param("status") EnrollmentStatus status);

}
