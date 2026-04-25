package com.example.enrollment.common.exception;

import org.springframework.http.HttpStatus;

public class EnrollmentNotFoundException extends BusinessException {
    public EnrollmentNotFoundException() {
        super(
                "ENROLLMENT_NOT_FOUND",
                "수강 신청 내역을 찾을 수 없습니다.",
                HttpStatus.NOT_FOUND);
    }
}
