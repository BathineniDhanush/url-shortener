package com.example.shortener.link.error;

public class InvalidExpirationException extends RuntimeException {
    public InvalidExpirationException(String message) {
        super(message);
    }
}
