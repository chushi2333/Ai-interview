package com.chushi.template.exceptions;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int status;

    private final int code;

    public BusinessException(int status, String message) {
        this(status, status, message);
    }

    public BusinessException(int status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
