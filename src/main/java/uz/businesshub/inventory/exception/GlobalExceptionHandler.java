package uz.businesshub.inventory.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String ERROR_BASE = "https://businesshub.uz/errors/";


    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "Data conflict",
                "Request conflicts with existing data", "DATA_CONFLICT");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "Concurrent modification",
                "Resource was modified by another request. Reload and retry.", "CONCURRENT_MODIFICATION");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error",
                "Unexpected error occurred", "INTERNAL_ERROR");
    }

    @ExceptionHandler(VersionConflictException.class)
    public ProblemDetail handleVersionConflict(VersionConflictException ex) {
        return problem(HttpStatus.CONFLICT, "Version conflict", ex.getMessage(), "VERSION_CONFLICT");
    }

    @ExceptionHandler(MissingTenantException.class)
    public ProblemDetail handleMissingTenant(MissingTenantException ex) {
        return problem(HttpStatus.FORBIDDEN, "Tenant missing", ex.getMessage(), "TENANT_MISSING");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Access denied",
                "You do not have permission to perform this action", "ACCESS_DENIED");
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(ProductNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Product not found", ex.getMessage(), "PRODUCT_NOT_FOUND");
    }

    @ExceptionHandler(ProductInactiveException.class)
    public ProblemDetail handleProductInactive(ProductInactiveException ex) {
        return problem(HttpStatus.CONFLICT, "Product inactive", ex.getMessage(), "PRODUCT_INACTIVE");
    }

    @ExceptionHandler(DependencyUnavailableException.class)
    public ProblemDetail handleDependency(DependencyUnavailableException ex) {
        log.warn("Dependency failure: {}", ex.getMessage(), ex.getCause());
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Dependency unavailable",
                "A required service is temporarily unavailable. Please retry.", "DEPENDENCY_UNAVAILABLE");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fe.getField(),
                    fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid");
        }

        ProblemDetail body = ex.getBody();
        body.setType(URI.create(ERROR_BASE + "validation-error"));
        body.setTitle("Validation failed");
        body.setProperty("code", "VALIDATION_ERROR");
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, String code) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(ERROR_BASE + code.toLowerCase().replace('_', '-')));
        pd.setTitle(title);
        pd.setProperty("code", code);
        return pd;
    }
}