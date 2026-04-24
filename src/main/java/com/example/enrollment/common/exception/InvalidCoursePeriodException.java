package com.example.enrollment.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidCoursePeriodException extends BusinessException{

    public InvalidCoursePeriodException() {
        super(
                "Invalid_Course_Period",
                "종료일은 시작일 이후여야 합니다.",
                HttpStatus.BAD_REQUEST);
    }
}
