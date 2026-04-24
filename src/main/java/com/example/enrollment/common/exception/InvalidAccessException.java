package com.example.enrollment.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidAccessException extends BusinessException{
    public InvalidAccessException() {
        super(
                "INVALID_ACCESS",
                "잘못된 접근입니다.",
                HttpStatus.FORBIDDEN);
    }
}
