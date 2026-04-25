package com.example.enrollment.common.exception;

import org.springframework.http.HttpStatus;

public class AlreadyEnrolledException extends BusinessException{
    public AlreadyEnrolledException() {
        super(
                "ALREADY_ENROLLED"
                , "이미 신정한 강의입니다",
                HttpStatus.CONFLICT);
    }
}
