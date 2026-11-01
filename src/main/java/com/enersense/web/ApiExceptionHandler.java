package com.enersense.web;

import com.enersense.service.MeterNotFoundException;
import com.enersense.service.NoReadingsException;
import com.enersense.service.SimulationNotStartedException;
import com.enersense.web.dto.ApiResponses.ApiError;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Turns exceptions into a consistent JSON body.
 *
 * <p>Messages describe what the caller did wrong and never leak internals: an unexpected
 * failure returns a generic 500 while the detail goes to the log.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MeterNotFoundException.class)
    public ResponseEntity<ApiError> notFound(MeterNotFoundException ex) {
        // 404 rather than 403: whether a meter exists is not something an unauthorised
        // caller should be able to probe.
        return build(HttpStatus.NOT_FOUND, "No such meter for this consumer");
    }

    @ExceptionHandler(SimulationNotStartedException.class)
    public ResponseEntity<ApiError> notStarted(SimulationNotStartedException ex) {
        return build(HttpStatus.CONFLICT, "Simulation not started. POST /api/simulation/start first.");
    }

    @ExceptionHandler(NoReadingsException.class)
    public ResponseEntity<ApiError> noReadings(NoReadingsException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Not enough readings yet to answer this.");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class,
            ConstraintViolationException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiError> badRequest(Exception ex) {
        String message = ex instanceof IllegalArgumentException
                ? ex.getMessage()
                : "Invalid or missing request parameters";
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception ex) {
        org.slf4j.LoggerFactory.getLogger(ApiExceptionHandler.class)
                .error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong");
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), status.getReasonPhrase(), message, LocalDateTime.now()));
    }
}
