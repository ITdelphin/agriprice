package com.agriprice.util;

/**
 * Thrown when an operation references a product ID or name that does not exist in the database.
 */
public class UnknownProductException extends Exception {
    private static final long serialVersionUID = 1L;

    public UnknownProductException(String message) {
        super(message);
    }

    public UnknownProductException(String message, Throwable cause) {
        super(message, cause);
    }
}
