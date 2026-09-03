package com.example.shortener.link.error;

public class InvalidDestinationUrlException extends RuntimeException {
    public InvalidDestinationUrlException(String message) {
        super(message);
    }

    public InvalidDestinationUrlException(String message, Throwable cause) {
        super(message, cause);
    }
}
