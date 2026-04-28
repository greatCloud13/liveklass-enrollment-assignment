package com.example.enrollment.common.exception;

import com.example.enrollment.domain.enrollment.EnrollmentStatus;
import org.springframework.http.HttpStatus;

public class EnrollmentStatusNotWaiting extends BusinessException{

    public EnrollmentStatusNotWaiting(EnrollmentStatus currentStatus) {
        super(
                "ENROLLMENT_STATUS_IS_NOT_WAITING",
                String.format("대기중인 수강신청이 아닙니다. 현재상태: %s", currentStatus.name())
                , HttpStatus.CONFLICT);
    }
}
