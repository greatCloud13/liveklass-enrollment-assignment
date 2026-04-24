package com.example.enrollment.common.exception;

import org.springframework.http.HttpStatus;

public class CourseNotFoundException extends BusinessException{
    public CourseNotFoundException() {
        super(
                "Course_Not_Found",
                "해당하는 강의를 찾을 수 없습니다.",
                HttpStatus.NOT_FOUND);
    }
}
