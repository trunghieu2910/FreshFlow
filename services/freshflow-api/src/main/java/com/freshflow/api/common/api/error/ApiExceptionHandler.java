package com.freshflow.api.common.api.error;

import com.freshflow.api.catalog.application.exception.CatalogErrorCode;
import com.freshflow.api.catalog.application.exception.CatalogNotFoundException;
import com.freshflow.api.catalog.application.exception.CatalogRuleViolationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(CatalogNotFoundException.class)
  ResponseEntity<ApiErrorResponse> handleNotFound(
      CatalogNotFoundException exception, HttpServletRequest request) {
    return response(
        HttpStatus.NOT_FOUND, exception.getCode(), exception.getMessage(), request, List.of());
  }

  @ExceptionHandler(CatalogRuleViolationException.class)
  ResponseEntity<ApiErrorResponse> handleRule(
      CatalogRuleViolationException exception, HttpServletRequest request) {

    CatalogErrorCode errorCode = exception.getErrorCode();

    // Sử dụng Switch Expression của Java hiện đại
    HttpStatus status =
        switch (errorCode) {
          case VARIANT_DUPLICATE, CONFLICT -> HttpStatus.CONFLICT;
          case STORE_ACCESS_DENIED ->
              HttpStatus.FORBIDDEN; // Thêm PRODUCT_ACCESS_DENIED nếu bạn đã khai báo trong enum
          default -> HttpStatus.BAD_REQUEST;
        };

    return response(
        status,
        exception.getCode(), // Thay vì errorCode.value()
        exception.getMessage(),
        request,
        List.of());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiErrorResponse> handleValidation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    List<ApiErrorResponse.FieldError> errors =
        exception.getBindingResult().getFieldErrors().stream()
            .sorted(
                Comparator.comparing(FieldError::getField)
                    .thenComparing(
                        FieldError::getDefaultMessage, Comparator.nullsFirst(String::compareTo)))
            .map(
                error ->
                    new ApiErrorResponse.FieldError(
                        error.getField(), safeMessage(error.getDefaultMessage())))
            .toList();
    return response(
        HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", request, errors);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ApiErrorResponse> handleConstraintViolation(
      ConstraintViolationException exception, HttpServletRequest request) {
    List<ApiErrorResponse.FieldError> errors =
        exception.getConstraintViolations().stream()
            .map(
                violation ->
                    new ApiErrorResponse.FieldError(
                        violation.getPropertyPath().toString(), violation.getMessage()))
            .sorted(Comparator.comparing(ApiErrorResponse.FieldError::field))
            .toList();
    return response(
        HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", request, errors);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ApiErrorResponse> handleMalformedRequest(
      HttpMessageNotReadableException exception, HttpServletRequest request) {
    return response(
        HttpStatus.BAD_REQUEST,
        "MALFORMED_REQUEST",
        "Request body is malformed",
        request,
        List.of());
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  ResponseEntity<ApiErrorResponse> handleMissingHeader(
      MissingRequestHeaderException exception, HttpServletRequest request) {
    List<ApiErrorResponse.FieldError> errors =
        List.of(new ApiErrorResponse.FieldError(exception.getHeaderName(), "header is required"));
    return response(
        HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", request, errors);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ApiErrorResponse> handleConflict(
      DataIntegrityViolationException exception, HttpServletRequest request) {
    return response(
        HttpStatus.CONFLICT,
        "CATALOG_CONFLICT",
        "Catalog resource conflicts with existing data",
        request,
        List.of());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ApiErrorResponse> handleIllegalArgument(
      IllegalArgumentException exception, HttpServletRequest request) {
    return response(
        HttpStatus.BAD_REQUEST,
        "INVALID_REQUEST",
        safeMessage(exception.getMessage()),
        request,
        List.of());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiErrorResponse> handleUnexpected(
      Exception exception, HttpServletRequest request) {
    return response(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_ERROR",
        "Internal server error",
        request,
        List.of());
  }

  private ResponseEntity<ApiErrorResponse> response(
      HttpStatus status,
      String code,
      String message,
      HttpServletRequest request,
      List<ApiErrorResponse.FieldError> fieldErrors) {
    return ResponseEntity.status(status)
        .body(
            new ApiErrorResponse(
                code, message, request.getRequestURI(), Instant.now(), fieldErrors));
  }

  private static String safeMessage(String message) {
    return message == null || message.isBlank() ? "Invalid value" : message;
  }
}
