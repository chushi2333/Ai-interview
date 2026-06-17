package com.chushi.aiinterview.exceptions;

public class CacheException extends RuntimeException {
    public CacheException(String message) {
        super(message);
    }

    public CacheException(Throwable cause) {
        super(cause);
    }
}
