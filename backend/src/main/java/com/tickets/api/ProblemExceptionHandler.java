package com.tickets.api;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.tickets.domain.IllegalTicketTransitionException;
import com.tickets.domain.TicketPriority;
import com.tickets.domain.TicketStatus;
import com.tickets.service.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ProblemExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        Map<String, String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        ProblemExceptionHandler::resolveMessage,
                        (first, ignored) -> first,
                        LinkedHashMap::new));

        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "One or more request fields are invalid");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
        Map<String, String> errors = exception.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage(),
                        (first, ignored) -> first,
                        LinkedHashMap::new));

        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "One or more request values are invalid");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableMessage(HttpMessageNotReadableException exception) {
        Throwable cause = exception.getMostSpecificCause();
        if (cause instanceof InvalidFormatException invalidFormat) {
            String field = fieldName(invalidFormat);
            String message = enumMessage(invalidFormat.getTargetType(), field);
            if (field != null && message != null) {
                ProblemDetail problem = createProblem(
                        HttpStatus.BAD_REQUEST,
                        "Validation failed",
                        "One or more request fields are invalid");
                problem.setProperty("errors", Map.of(field, message));
                return problem;
            }
        }
        return createProblem(
                HttpStatus.BAD_REQUEST,
                "Malformed request",
                "The request body is missing or contains invalid JSON values");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String field = exception.getName() == null ? "value" : exception.getName();
        Class<?> requiredType = exception.getRequiredType();
        String message = enumMessage(requiredType, field);
        if (message == null && "ticketId".equals(field)) {
            message = "Ticket id must be a valid UUID";
        }
        if (message == null) {
            message = "Invalid value";
        }
        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "One or more request values are invalid");
        problem.setProperty("errors", Map.of(field, message));
        return problem;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials() {
        return createProblem(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "The supplied credentials are invalid");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException exception) {
        return createProblem(HttpStatus.NOT_FOUND, "Not found", exception.getMessage());
    }

    @ExceptionHandler(IllegalTicketTransitionException.class)
    public ProblemDetail handleIllegalTransition(IllegalTicketTransitionException exception) {
        String detail = "Transition from " + exception.getCurrentStatus()
                + " to " + exception.getRequestedStatus() + " is not allowed";
        return createProblem(HttpStatus.UNPROCESSABLE_ENTITY, "Illegal ticket status transition", detail);
    }

    private static String fieldName(InvalidFormatException exception) {
        return exception.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(name -> name != null && !name.isBlank())
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private static String enumMessage(Class<?> targetType, String field) {
        if (targetType == null) {
            return null;
        }
        if (TicketPriority.class.isAssignableFrom(targetType) || "priority".equals(field)) {
            return "Priority must be one of LOW, MEDIUM, HIGH";
        }
        if (TicketStatus.class.isAssignableFrom(targetType) || "status".equals(field)) {
            return "Status must be one of OPEN, IN_PROGRESS, RESOLVED, CLOSED, CANCELLED";
        }
        return null;
    }

    private static ProblemDetail createProblem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }

    private static String resolveMessage(DefaultMessageSourceResolvable error) {
        return error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage();
    }
}
