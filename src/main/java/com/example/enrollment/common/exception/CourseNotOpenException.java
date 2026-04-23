package com.example.enrollment.common.exception;

import org.springframework.http.HttpStatus;

public class CourseNotOpenException extends BusinessException{

    public CourseNotOpenException() {
        super("COURSE_NOT_OPEN_EXCEPTION",
                "오픈되지 않은 강의입니다.",
                HttpStatus.BAD_REQUEST);
    }
}
