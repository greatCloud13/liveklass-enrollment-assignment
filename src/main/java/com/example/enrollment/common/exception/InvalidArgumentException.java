package com.example.enrollment.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidArgumentException extends BusinessException{

    public InvalidArgumentException() {
        super(
                "INVALID_ARGUMENT",
                "요청 데이터 유효성 검증에 실패하였습니다.",
                HttpStatus.BAD_REQUEST);
    }
}
