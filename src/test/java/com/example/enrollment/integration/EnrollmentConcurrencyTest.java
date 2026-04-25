package com.example.enrollment.integration;

import com.example.enrollment.application.EnrollmentService;
import com.example.enrollment.domain.course.Course;
import com.example.enrollment.domain.course.CourseRepository;
import com.example.enrollment.domain.enrollment.EnrollmentRepository;
import com.example.enrollment.domain.enrollment.EnrollmentStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EnrollmentConcurrencyTest {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    private Long courseId;

    @BeforeEach
    void setUp() {
        Course course = Course.builder()
                .title("마감 임박 강의")
                .description("테스트 강의 설명")
                .price(BigDecimal.valueOf(10000))
                .maxCapacity(1)   // 정원 1명
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .instructorId(1L)
                .build();
        course.open();
        courseId = courseRepository.save(course).getId();
    }

    @AfterEach
    void tearDown() {
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
    }

    @Test
    @DisplayName("동시에 10명이 마지막 1자리에 신청하면 1명만 성공해야 한다")
    void concurrentEnroll_onlyOneSucceeds() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);   // 동시 출발 신호
        CountDownLatch doneLatch = new CountDownLatch(threadCount); // 전원 완료 대기

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 1;
            executor.submit(() -> {
                try {
                    startLatch.await();  // 신호 전까지 대기
                    enrollmentService.enroll(courseId, userId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 전 스레드 동시 출발
        doneLatch.await();      // 전원 완료까지 대기
        executor.shutdown();

        // [Then] 성공은 반드시 1건
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(threadCount - 1);

        // DB에도 실제로 1건만 저장되었는지 이중 검증
        int actualCount = enrollmentRepository.countByCourseIdAndStatusIn(
                courseId, List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED));
        assertThat(actualCount).isEqualTo(1);
    }
}