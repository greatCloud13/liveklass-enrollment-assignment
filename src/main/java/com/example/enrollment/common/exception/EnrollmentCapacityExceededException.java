package com.example.enrollment.common.exception;

import org.springframework.http.HttpStatus;

public class EnrollmentCapacityExceededException extends BusinessException{

    public EnrollmentCapacityExceededException() {
        super("CAPACITY_EXCEEDED",
                "수강 정원이 초과되었습니다.",
                HttpStatus.CONFLICT);
    }
}
