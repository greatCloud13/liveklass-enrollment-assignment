package com.example.enrollment.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidStatusTransitionException extends BusinessException{
    public InvalidStatusTransitionException(String from, String to) {
        super("INVALID_STATUS_TRANSITION",
                String.format("상태를 변경할 수 없습니다. %s -> %s", from, to),
                HttpStatus.BAD_REQUEST
        );
    }
}
