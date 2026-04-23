package com.example.enrollment.domain.course;


import com.example.enrollment.common.exception.CourseNotOpenException;
import com.example.enrollment.common.exception.EnrollmentCapacityExceededException;
import com.example.enrollment.common.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

public class CourseTest {

    private Course course;
    private Course unlimitedCourse;

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

        unlimitedCourse = Course.builder()
                .title("수강인원 무제한 테스트 강의")
                .description("강의 설명")
                .price(BigDecimal.valueOf(50000))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .instructorId(1L)
                .build();
    }

    @Nested
    @DisplayName("open() 기능 테스트")
    class CourseOpenTest {

        @Test
        @DisplayName("DRAFT 상태에서 open() 호출 시 OPEN으로 전환된다.")
        void courseOpen_Success() {
            course.open();

            assertThat(course.getStatus()).isEqualTo(CourseStatus.OPEN);
        }

        @Test
        @DisplayName("DRAFT가 아닌 상태에서 open() 호출 시 예외가 발생한다")
        void open_Fail_InvalidStatus() {
            course.open(); // OPEN 상태로 전환

            assertThatThrownBy(() -> course.open())
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("close() 기능 테스트")
    class CourseCloseTest{

        @Test
        @DisplayName("OPEN 상태에서 close() 호출 시 CLOSED로 전환된다")
        void courseClose_Success(){
            course.open();

            course.close();

            assertThat(course.getStatus()).isEqualTo(CourseStatus.CLOSED);
        }

        @Test
        @DisplayName("OPEN이 아닌 상태에서 close() 호출 시 예외 발생")
        void close_Fail_InvalidStatus(){
            course.open();
            course.close();

            assertThatThrownBy(()->course.close())
                    .isInstanceOf(InvalidStatusTransitionException.class);

        }

    }

    @Nested
    @DisplayName("validateEnrollable() 기능 테스트")
    class CourseValidateEnrollableTest{

        @Test
        @DisplayName("OPEN인 상태에서 예외 발생하지 않음")
        void courseValidateEnrollable_success(){
            course.open();

            assertThatNoException().isThrownBy(()-> course.validateEnrollable());
        }

        @Test
        @DisplayName("CLOSE인 상태에서 validateEnrollable() 호출 시 예외 발생한다.")
        void courseValidateEnrollable_Fail_Course_Closed(){
            course.open();
            course.close();

            assertThatThrownBy(()-> course.validateEnrollable())
                    .isInstanceOf(CourseNotOpenException.class);
        }

        @Test
        @DisplayName("DRAFT인 상태에서 validateEnrollable() 호출 시 예외 발생한다.")
        void courseValidateEnrollable_Fail_Course_DRAFT(){

            assertThatThrownBy(()-> course.validateEnrollable())
                    .isInstanceOf(CourseNotOpenException.class);
        }

    }

    @Nested
    @DisplayName("validateCapacity() 기능 테스트")
    class CourseValidateCapacityTest{

        @Test
        @DisplayName("현재 인원 < 정원인 경우 validateCapacity() 호출 시 예외 없음")
        void courseValidateCapacity_success(){
            int enrollmentUserCount = 200;

            assertThatNoException().isThrownBy(()->course.validateCapacity(enrollmentUserCount));
        }

        @Test
        @DisplayName("현재 인원 >= 정원인 경우 validateCapacity() 호출 시 예외가 발생한다.")
        void courseValidateCapacity_Fail_Over_Capacity(){
            int enrollmentUserCount = 500;

            assertThatThrownBy(() -> course.validateCapacity(enrollmentUserCount))
                    .isInstanceOf(EnrollmentCapacityExceededException.class);
        }

        @Test
        @DisplayName("maxCapacity가 null이면 정원 검증을 생략한다")
        void validateCapacity_Unlimited() {
            // maxCapacity 없이 생성 (무제한)
            assertThatNoException()
                    .isThrownBy(() -> unlimitedCourse.validateCapacity(9999));
        }

    }
}
