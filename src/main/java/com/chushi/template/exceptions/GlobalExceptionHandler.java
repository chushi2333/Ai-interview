package com.chushi.template.exceptions;

import com.chushi.template.commons.vo.Response;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Response<?>> handleBusinessException(BusinessException e) {
        return ResponseEntity.status(e.getStatus()).body(Response.builder()
                .code(e.getCode())
                .message(e.getMessage())
                .data(null)
                .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response<?>> handleValidationException(MethodArgumentNotValidException e) {
        var errors = new LinkedHashMap<String, String>();
        e.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        return ResponseEntity.badRequest().body(Response.builder()
                .code(2)
                .message("参数校验失败")
                .data(errors)
                .build());
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            HttpRequestMethodNotSupportedException.class,
    })
    public ResponseEntity<Response<?>> handleBadRequestException(Exception e) {
        return ResponseEntity.badRequest().body(Response.builder()
                .code(3)
                .message(e.getMessage())
                .data(null)
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<?>> handleGenericException(Exception e) {
        log.error("GenericException: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).body(Response.builder()
                .code(-1)
                .message("Internal server error")
                .data(null)
                .build());
    }
}
