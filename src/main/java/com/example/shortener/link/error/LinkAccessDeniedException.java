package com.example.shortener.link.error;

public class LinkAccessDeniedException extends RuntimeException {
    public LinkAccessDeniedException() {
        super("A valid link owner token is required");
    }
}
