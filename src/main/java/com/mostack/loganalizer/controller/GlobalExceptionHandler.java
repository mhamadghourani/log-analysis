package com.mostack.loganalizer.controller;

import com.mostack.loganalizer.record.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CompletionException.class)
    public ResponseEntity<ApiError> handleCompletion(CompletionException ex, HttpServletRequest req) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        return dispatch(cause, req);
    }

    @ExceptionHandler({MissingRequestHeaderException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> handleMissingParam(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Missing required parameter", ex.getMessage(), req);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return build(HttpStatus.BAD_REQUEST, "Invalid request body", message, req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Invalid input", ex.getMessage(), req);
    }

    @ExceptionHandler({TimeoutException.class, AsyncRequestTimeoutException.class})
    public ResponseEntity<ApiError> handleTimeout(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.REQUEST_TIMEOUT, "Timed out",
                "Timed out waiting for an error log to be written.", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Analysis failed", ex.getMessage(), req);
    }

    private ResponseEntity<ApiError> dispatch(Throwable cause, HttpServletRequest req) {
        if (cause instanceof IllegalArgumentException iae) return handleIllegalArgument(iae, req);
        if (cause instanceof TimeoutException te) return handleTimeout(te, req);
        return handleGeneric(cause instanceof Exception e ? e : new Exception(cause), req);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String error, String message, HttpServletRequest req) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), error, message, req.getRequestURI()));
    }
}