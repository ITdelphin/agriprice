package com.agriprice.util;

/**
 * Thrown when an agricultural price submission violates basic value constraints
 * (e.g. price <= 0, empty or non-numeric values).
 */
public class InvalidPriceException extends Exception {
    private static final long serialVersionUID = 1L;

    public InvalidPriceException(String message) {
        super(message);
    }

    public InvalidPriceException(String message, Throwable cause) {
        super(message, cause);
    }
}
