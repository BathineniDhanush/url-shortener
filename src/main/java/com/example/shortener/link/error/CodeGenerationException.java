package com.example.shortener.link.error;

public class CodeGenerationException extends RuntimeException {
    public CodeGenerationException() {
        super("Unable to allocate a unique short code");
    }
}
