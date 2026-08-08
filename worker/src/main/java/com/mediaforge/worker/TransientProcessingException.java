package com.mediaforge.worker;

public class TransientProcessingException extends RuntimeException {
    public TransientProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}