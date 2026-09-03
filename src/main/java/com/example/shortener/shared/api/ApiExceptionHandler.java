package com.example.shortener.shared.api;

import com.example.shortener.link.error.AliasConflictException;
import com.example.shortener.link.error.CodeGenerationException;
import com.example.shortener.link.error.InvalidDestinationUrlException;
import com.example.shortener.link.error.InvalidExpirationException;
import com.example.shortener.link.error.LinkNotFoundException;
import com.example.shortener.link.error.LinkUnavailableException;
import com.example.shortener.link.error.LinkAccessDeniedException;
import com.example.shortener.link.error.ConcurrentLinkUpdateException;
import com.example.shortener.link.error.InvalidLinkUpdateException;
import com.example.shortener.link.error.RateLimitExceededException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({InvalidDestinationUrlException.class, InvalidExpirationException.class,
            InvalidLinkUpdateException.class})
    ProblemDetail handleBadRequest(RuntimeException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ProblemDetail handleMissingHeader(MissingRequestHeaderException exception) {
        return problem(HttpStatus.FORBIDDEN, "Link access denied", "A valid link owner token is required");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Request validation failed",
                "One or more request fields are invalid");
        List<String> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(AliasConflictException.class)
    ProblemDetail handleConflict(AliasConflictException exception) {
        return problem(HttpStatus.CONFLICT, "Short code conflict", exception.getMessage());
    }

    @ExceptionHandler(LinkAccessDeniedException.class)
    ProblemDetail handleForbidden(LinkAccessDeniedException exception) {
        return problem(HttpStatus.FORBIDDEN, "Link access denied", exception.getMessage());
    }

    @ExceptionHandler(ConcurrentLinkUpdateException.class)
    ProblemDetail handleConcurrentUpdate(ConcurrentLinkUpdateException exception) {
        return problem(HttpStatus.CONFLICT, "Concurrent link update", exception.getMessage());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<ProblemDetail> handleRateLimit(RateLimitExceededException exception) {
        ProblemDetail detail = problem(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded", exception.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", Long.toString(exception.retryAfterSeconds()))
                .body(detail);
    }

    @ExceptionHandler(CodeGenerationException.class)
    ProblemDetail handleCodeGeneration(CodeGenerationException exception) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Code allocation unavailable", exception.getMessage());
    }

    @ExceptionHandler(LinkNotFoundException.class)
    ProblemDetail handleNotFound(LinkNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Short link not found", exception.getMessage());
    }

    @ExceptionHandler(LinkUnavailableException.class)
    ProblemDetail handleGone(LinkUnavailableException exception) {
        return problem(HttpStatus.GONE, "Short link unavailable", exception.getMessage());
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://url-shortener.example/problems/" + status.value()));
        return problem;
    }
}
