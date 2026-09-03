package com.example.shortener.link.error;

public class InvalidLinkUpdateException extends RuntimeException {
    public InvalidLinkUpdateException(String message) {
        super(message);
    }
}
