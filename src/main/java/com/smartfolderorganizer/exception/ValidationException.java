package com.smartfolderorganizer.exception;

/**
 * Custom runtime exception thrown when domain rules or input validations fail.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
