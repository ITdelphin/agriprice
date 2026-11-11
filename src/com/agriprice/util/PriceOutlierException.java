package com.agriprice.util;

/**
 * Thrown when an agricultural price deviates significantly from the 7-day trailing average
 * for a specific market and product, exceeding the configured outlier threshold.
 */
public class PriceOutlierException extends Exception {
    private static final long serialVersionUID = 1L;

    private final double submittedPrice;
    private final double averagePrice;
    private final double deviationPercentage;
    private final double threshold;

    public PriceOutlierException(String message, double submittedPrice, double averagePrice, double deviationPercentage, double threshold) {
        super(message);
        this.submittedPrice = submittedPrice;
        this.averagePrice = averagePrice;
        this.deviationPercentage = deviationPercentage;
        this.threshold = threshold;
    }

    public double getSubmittedPrice() {
        return submittedPrice;
    }

    public double getAveragePrice() {
        return averagePrice;
    }

    public double getDeviationPercentage() {
        return deviationPercentage;
    }

    public double getThreshold() {
        return threshold;
    }
}
