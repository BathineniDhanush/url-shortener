package com.example.shortener.link.error;

public class LinkUnavailableException extends RuntimeException {
    public LinkUnavailableException(String code) {
        super("Short link is no longer available: " + code);
    }
}
