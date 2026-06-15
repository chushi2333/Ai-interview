package com.chushi.aiinterview.exceptions;

import com.chushi.aiinterview.commons.vo.Response;
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
    // 处理自定义业务异常
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Response<?>> handleBusinessException(BusinessException e) {
        var response = Response
                .builder()
                .code(e.getCode())
                .message(e.getMessage())
                .data(null)
                .build();

        return ResponseEntity.status(e.getStatus()).body(response);
    }

    // 处理参数验证异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response<LinkedHashMap<String, String>>> handleValidationException(MethodArgumentNotValidException e) {
        var fieldErrors = new LinkedHashMap<String, String>();
        e.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(
                        error.getField(),
                        error.getDefaultMessage() == null ? "参数格式不正确" : error.getDefaultMessage()
                )
        );

        var response = Response
                .<LinkedHashMap<String, String>>builder()
                .code(2)
                .message("参数校验失败")
                .data(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    // 处理一般非内部错误异常
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            HttpRequestMethodNotSupportedException.class,
    })
    public ResponseEntity<Response<?>> handleGenericNotInternalException(Exception e) {
        var response = Response
                .builder()
                .code(3)
                .message("Exception: " + e.getMessage())
                .data(null)
                .build();

        return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(response);
    }

    // 处理其他所有异常
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<?>> handleGenericException(Exception e) {
        log.error("GenericException: {}", e.getMessage(), e);

        var response = Response
                .builder()
                .code(-1)
                .message("InternalServerError: " + e.getMessage())
                .data(null)
                .build();

        return ResponseEntity.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).body(response);
    }
}
