package com.mediaforge.worker;

public class PermanentProcessingException extends RuntimeException {
    public PermanentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}