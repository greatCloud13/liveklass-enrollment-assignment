package com.example.enrollment.common.exception;

import org.springframework.http.HttpStatus;

public class EnrollmentCancelNotAllowedException extends BusinessException{
    public EnrollmentCancelNotAllowedException() {
        super(
                "ENROLLMENT_CANCEL_NOT_ALLOWED",
                "수강 취소가 불가능합니다.",
                HttpStatus.BAD_REQUEST);
    }
}
