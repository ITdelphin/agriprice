package com.agriprice.util;

import java.util.regex.Pattern;

/**
 * Validation utility enforcing agricultural price constraints, outlier rules,
 * input hygiene, and cross-site scripting (XSS) prevention.
 */
public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+?[0-9\\s-]{9,15}$"
    );

    /**
     * Enforces that agricultural prices are strictly positive numbers.
     */
    public static void validatePrice(double price) throws InvalidPriceException {
        if (Double.isNaN(price) || Double.isInfinite(price) || price <= 0) {
            throw new InvalidPriceException("Agricultural price must be strictly greater than 0 RWF. Received: " + price);
        }
    }

    /**
     * Calculates percentage deviation and validates against the configured outlier threshold.
     * Formula: deviation = ABS(newPrice - averagePrice) / averagePrice * 100
     */
    public static double calculateDeviation(double newPrice, double averagePrice) {
        if (averagePrice <= 0) {
            return 0.0;
        }
        return (Math.abs(newPrice - averagePrice) / averagePrice) * 100.0;
    }

    /**
     * Checks if a price is an outlier relative to the historical average.
     */
    public static void validatePriceOutlier(double newPrice, double averagePrice, double threshold) 
            throws PriceOutlierException {
        if (averagePrice > 0) {
            double deviation = calculateDeviation(newPrice, averagePrice);
            if (deviation > threshold) {
                String msg = String.format(
                    "Price %.2f RWF deviates %.2f%% from the 7-day average (%.2f RWF), exceeding the %.1f%% allowed threshold.",
                    newPrice, deviation, averagePrice, threshold
                );
                throw new PriceOutlierException(msg, newPrice, averagePrice, deviation, threshold);
            }
        }
    }

    /**
     * Validates email formatting.
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validates telephone formatting.
     */
    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    /**
     * Escapes dangerous HTML characters to prevent Cross-Site Scripting (XSS).
     */
    public static String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;")
                    .replace("/", "&#x2F;");
    }

    /**
     * Strips potentially harmful tags and trims input.
     */
    public static String sanitizeString(String input) {
        if (input == null) {
            return "";
        }
        return input.trim();
    }
}
