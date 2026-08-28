package com.freshflow.api.common.api.error;

import com.freshflow.api.catalog.application.exception.CatalogErrorCode;
import com.freshflow.api.catalog.application.exception.CatalogNotFoundException;
import com.freshflow.api.catalog.application.exception.CatalogRuleViolationException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(CatalogNotFoundException.class)
  ResponseEntity<ApiErrorResponse> handleNotFound(
      CatalogNotFoundException exception, HttpServletRequest request) {
    return response(HttpStatus.NOT_FOUND, exception.getCode(), exception.getMessage(), request);
  }

  @ExceptionHandler(CatalogRuleViolationException.class)
  ResponseEntity<ApiErrorResponse> handleRule(
      CatalogRuleViolationException exception, HttpServletRequest request) {
    HttpStatus status =
        switch (exception.getErrorCode()) {
          case STORE_ACCESS_DENIED -> HttpStatus.FORBIDDEN;
          case VARIANT_DUPLICATE, CONFLICT -> HttpStatus.CONFLICT;
          default -> HttpStatus.BAD_REQUEST;
        };
    return response(status, exception.getCode(), exception.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiErrorResponse> handleValidation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
    return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
  }

  @ExceptionHandler({IllegalArgumentException.class, NumberFormatException.class})
  ResponseEntity<ApiErrorResponse> handleIllegalArgument(
      RuntimeException exception, HttpServletRequest request) {
    return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", exception.getMessage(), request);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ApiErrorResponse> handleConflict(
      DataIntegrityViolationException exception, HttpServletRequest request) {
    return response(
        HttpStatus.CONFLICT,
        CatalogErrorCode.CONFLICT.code(),
        "The request conflicts with existing catalog data",
        request);
  }

  private ResponseEntity<ApiErrorResponse> response(
      HttpStatus status, String code, String message, HttpServletRequest request) {
    return ResponseEntity.status(status)
        .body(new ApiErrorResponse(code, message, request.getRequestURI(), Instant.now()));
  }
}
