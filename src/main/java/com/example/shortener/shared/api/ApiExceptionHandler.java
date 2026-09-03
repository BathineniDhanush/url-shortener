package com.example.shortener.shared.api;

import com.example.shortener.link.error.AliasConflictException;
import com.example.shortener.link.error.CodeGenerationException;
import com.example.shortener.link.error.InvalidDestinationUrlException;
import com.example.shortener.link.error.InvalidExpirationException;
import com.example.shortener.link.error.LinkNotFoundException;
import com.example.shortener.link.error.LinkUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({InvalidDestinationUrlException.class, InvalidExpirationException.class})
    ProblemDetail handleBadRequest(RuntimeException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage());
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
