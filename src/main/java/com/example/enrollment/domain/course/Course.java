package com.example.enrollment.domain.course;

import com.example.enrollment.common.exception.CourseNotOpenException;
import com.example.enrollment.common.exception.EnrollmentCapacityExceededException;
import com.example.enrollment.common.exception.InvalidStatusTransitionException;
import com.example.enrollment.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Table(
        name="course",
        indexes = {
                @Index(name = "idx_course_status", columnList = "status")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // 외부 직접 생성 방지
public class Course extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column
    private Integer maxCapacity;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CourseStatus status;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Long instructorId;


    public void open(){
        if(this.status != CourseStatus.DRAFT){
            throw new InvalidStatusTransitionException(this.status.toString(), CourseStatus.OPEN.toString());
        }
        this.status = CourseStatus.OPEN;
    }

    public void close(){
        if(this.status != CourseStatus.OPEN){
            throw new InvalidStatusTransitionException(this.status.toString(), CourseStatus.CLOSED.toString());
        }
        this.status = CourseStatus.CLOSED;
    }

    public void validateEnrollable(){
        if(this.status != CourseStatus.OPEN){
            throw new CourseNotOpenException();
        }
    }

    public void validateCapacity(int currentCount) {
        // maxCapacity가 null이면 무제한 — 정원 검증 생략
        if (this.maxCapacity != null && currentCount >= this.maxCapacity) {
            throw new EnrollmentCapacityExceededException();
        }
    }

    @Builder
    public Course(String title, String description, BigDecimal price,
                                Integer maxCapacity, LocalDate startDate,
                                LocalDate endDate, Long instructorId) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.maxCapacity = maxCapacity;
        this.status = CourseStatus.DRAFT;
        this.startDate = startDate;
        this.endDate = endDate;
        this.instructorId = instructorId;
    }

}
