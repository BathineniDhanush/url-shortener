package com.example.shortener.link.error;

public class AliasConflictException extends RuntimeException {
    public AliasConflictException(String alias) {
        super("Short code is already in use: " + alias);
    }
}
