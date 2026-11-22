/* Shared client-side pricing checks used by price submission and QUnit. */
window.AgriPricingRules = Object.freeze({
    validatePrice(price) {
        if (typeof price !== 'number' || !Number.isFinite(price) || price <= 0) {
            throw new Error('Agricultural price must be greater than zero RWF.');
        }
        return true;
    },

    calculateDeviation(price, average) {
        if (!Number.isFinite(average) || average <= 0 || !Number.isFinite(price)) return 0;
        return Math.abs(price - average) / average * 100;
    },

    isPriceOutlier(price, average, threshold = 25) {
        if (!Number.isFinite(average) || average <= 0) return false;
        return this.calculateDeviation(price, average) > threshold;
    },

    calculateAverage(prices) {
        if (!Array.isArray(prices) || prices.length === 0) return 0;
        return prices.reduce((sum, value) => sum + value, 0) / prices.length;
    },

    isDuplicateSubmission(reports, marketId, productId, date) {
        return reports.some(report => Number(report.marketId) === Number(marketId)
            && Number(report.productId) === Number(productId)
            && report.reportDate === date);
    }
});
