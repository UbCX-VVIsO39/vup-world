package com.example.vupworld.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(GameException.class)
    ApiResponse<Object> handleGameException(GameException exception, HttpServletResponse response) {
        if ("UNAUTHORIZED".equals(exception.code())) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
        return ApiResponse.error(exception.code(), exception.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiResponse<Object> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ApiResponse.error("VALIDATION_ERROR", message, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiResponse<Object> handleMissingParam(MissingServletRequestParameterException exception) {
        return ApiResponse.error("MISSING_PARAMETER", "缺少必要参数: " + exception.getParameterName(), null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiResponse<Object> handleNotReadable(HttpMessageNotReadableException exception) {
        return ApiResponse.error("INVALID_REQUEST_BODY", "请求体格式错误", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    ApiResponse<Object> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        return ApiResponse.error("METHOD_NOT_ALLOWED", "不支持的请求方法: " + exception.getMethod(), null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ApiResponse<Object> handleNoResource(NoResourceFoundException exception) {
        return ApiResponse.error("NOT_FOUND", "资源不存在: " + exception.getResourcePath(), null);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ApiResponse<Object> handleNotFound(NoHandlerFoundException exception) {
        return ApiResponse.error("NOT_FOUND", "接口不存在: " + exception.getRequestURL(), null);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ApiResponse<Object> handleException(
            Exception exception,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (isClientAbort(exception)) {
            log.debug("Client disconnected while streaming response: {}", request.getRequestURI());
            return ApiResponse.error("CLIENT_DISCONNECTED", "客户端已断开连接", null);
        }
        if (response.isCommitted()) {
            log.error("Unexpected error after response was committed: {}", request.getRequestURI(), exception);
            return ApiResponse.error("RESPONSE_COMMITTED", "响应已提交，无法写入错误信息", null);
        }
        if (hasNonJsonContentType(response)) {
            response.resetBuffer();
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        }
        log.error("Unexpected error", exception);
        return ApiResponse.error("INTERNAL_ERROR", "服务器内部错误", null);
    }

    private boolean hasNonJsonContentType(HttpServletResponse response) {
        String contentType = response.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            return !MediaType.APPLICATION_JSON.isCompatibleWith(mediaType)
                    && !mediaType.getSubtype().endsWith("+json");
        } catch (IllegalArgumentException exception) {
            return true;
        }
    }

    private boolean isClientAbort(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if ("ClientAbortException".equals(current.getClass().getSimpleName())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
