/*
 * Copyright 2026 IQKV Foundation Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iqkv.foundation.billingservice.infrastructure.config;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.iqkv.foundation.billingservice.plan.PlanScopeMismatchException;
import com.iqkv.foundation.billingservice.shared.exception.DuplicateResourceException;
import com.iqkv.foundation.billingservice.shared.exception.MessagingException;
import com.iqkv.foundation.billingservice.shared.exception.PaymentGatewayException;
import com.iqkv.foundation.billingservice.shared.exception.ResourceNotFoundException;
import com.iqkv.foundation.billingservice.shared.exception.TenantContextMismatchException;
import com.iqkv.foundation.billingservice.shared.exception.WebhookProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String MDC_CORRELATION_ID = "correlationId";

  private ProblemDetail problem(final String type,
                                final String title,
                                final int status,
                                final String detail,
                                final HttpServletRequest request) {
    final ProblemDetail pd = ProblemDetail.forStatus(status);
    pd.setType(URI.create(type));
    pd.setTitle(title);
    pd.setDetail(detail);
    pd.setInstance(URI.create(request.getRequestURI()));
    pd.setProperty("correlationId", MDC.get(MDC_CORRELATION_ID));
    pd.setProperty("requestId", "req-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
    return pd;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidation(final MethodArgumentNotValidException ex,
                                                        final HttpServletRequest request) {
    log.warn("Validation failed: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank", "Validation Failed", 400,
        "Request validation failed", request);
    final List<Map<String, String>> fields = ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> Map.of("field", fe.getField(), "message",
            fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid"))
        .toList();
    pd.setProperty("fields", fields);
    return ResponseEntity.badRequest().body(pd);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraintViolation(final ConstraintViolationException ex,
                                                                 final HttpServletRequest request) {
    log.warn("Constraint violation: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank", "Constraint Violation", 400,
        ex.getMessage(), request);
    return ResponseEntity.badRequest().body(pd);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleNotFound(final ResourceNotFoundException ex,
                                                      final HttpServletRequest request) {
    log.warn("Resource not found: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank", "Not Found", 404,
        ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
  }

  @ExceptionHandler(TenantContextMismatchException.class)
  public ResponseEntity<ProblemDetail> handleTenantContextMismatch(final TenantContextMismatchException ex,
                                                                   final HttpServletRequest request) {
    log.warn("Tenant context mismatch: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank", "Forbidden", 403,
        ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleAccessDenied(final AccessDeniedException ex,
                                                          final HttpServletRequest request) {
    log.warn("Access denied: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank", "Forbidden", 403,
        ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ProblemDetail> handleAuthentication(final AuthenticationException ex,
                                                            final HttpServletRequest request) {
    log.warn("Authentication failed: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank", "Unauthorized", 401,
        ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
  }

  @ExceptionHandler(DuplicateResourceException.class)
  public ResponseEntity<ProblemDetail> handleDuplicateResource(final DuplicateResourceException ex,
                                                               final HttpServletRequest request) {
    log.warn("Duplicate resource: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank", "Conflict", 409,
        ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
  }

  @ExceptionHandler(PlanScopeMismatchException.class)
  public ResponseEntity<ProblemDetail> handlePlanScopeMismatch(final PlanScopeMismatchException ex,
                                                               final HttpServletRequest request) {
    log.warn("Plan scope mismatch: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank", "Unprocessable Entity", 422,
        ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(pd);
  }

  @ExceptionHandler(WebhookProcessingException.class)
  public ResponseEntity<ProblemDetail> handleWebhookProcessing(final WebhookProcessingException ex,
                                                               final HttpServletRequest request) {
    log.error("Webhook processing error: {}", ex.getMessage(), ex);
    final ProblemDetail pd = problem("about:blank", "Unprocessable Entity", 422,
        ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(pd);
  }

  @ExceptionHandler(PaymentGatewayException.class)
  public ResponseEntity<ProblemDetail> handlePaymentGateway(final PaymentGatewayException ex,
                                                            final HttpServletRequest request) {
    log.error("Payment gateway error: {}", ex.getMessage(), ex);
    final ProblemDetail pd = problem("about:blank", "Bad Gateway", 502,
        ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(pd);
  }

  @ExceptionHandler(MessagingException.class)
  public ResponseEntity<ProblemDetail> handleMessaging(final MessagingException ex,
                                                       final HttpServletRequest request) {
    log.error("Messaging error: {}", ex.getMessage(), ex);
    final ProblemDetail pd = problem("about:blank", "Service Unavailable", 503,
        ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(pd);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleGeneral(final Exception ex,
                                                     final HttpServletRequest request) {
    log.error("Unhandled exception: {}", ex.getMessage(), ex);
    final ProblemDetail pd = problem("about:blank", "Internal Server Error", 500,
        "An unexpected error occurred", request);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
  }
}
