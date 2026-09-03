package com.example.shortener.link.error;

public class ConcurrentLinkUpdateException extends RuntimeException {
    public ConcurrentLinkUpdateException(String code) {
        super("Short link was modified by another request: " + code);
    }
}
