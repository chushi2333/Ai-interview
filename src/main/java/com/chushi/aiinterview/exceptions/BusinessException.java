package com.chushi.aiinterview.exceptions;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int status;
    private final int code;

    public BusinessException(Integer status, Integer code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public BusinessException(Integer status, String message) {
        super(message);
        this.status = status;
        this.code = 1;
    }
}
