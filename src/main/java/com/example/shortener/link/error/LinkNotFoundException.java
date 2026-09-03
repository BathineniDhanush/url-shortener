package com.example.shortener.link.error;

public class LinkNotFoundException extends RuntimeException {
    public LinkNotFoundException(String code) {
        super("Short link was not found: " + code);
    }
}
