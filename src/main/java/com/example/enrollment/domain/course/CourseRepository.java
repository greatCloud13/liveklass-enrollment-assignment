package com.example.enrollment.domain.course;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Page<Course> findByStatus(Pageable pageable, CourseStatus status);

}
