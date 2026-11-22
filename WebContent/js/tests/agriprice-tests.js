/**
 * AgriPrice Rwanda - Automated QUnit Test Suite
 * Academic Purpose: FAT Advanced Web and Internet Programming
 * Covers 12 comprehensive unit tests across 2 distinct modules.
 */

// The tests exercise the same client pricing rules used by the submit-price form.
const AgriTestUtils = AgriPricingRules;

// =============================================================================
// MODULE 1: Agricultural Pricing & Outlier Detection Algorithms
// =============================================================================
QUnit.module('Module 1: Agricultural Pricing & Outlier Detection', function() {

    QUnit.test('Test 1: Price validation accepts positive prices', function(assert) {
        assert.ok(AgriTestUtils.validatePrice(480.00), 'Standard maize price 480 RWF is valid.');
        assert.ok(AgriTestUtils.validatePrice(1250.50), 'Onion price 1250.50 RWF is valid.');
    });

    QUnit.test('Test 2: Price validation strictly rejects zero price', function(assert) {
        assert.throws(
            function() { AgriTestUtils.validatePrice(0.0); },
            /greater than (?:0|zero) RWF/i,
            'Zero price (0.0 RWF) throws validation error.'
        );
    });

    QUnit.test('Test 3: Price validation strictly rejects negative prices', function(assert) {
        assert.throws(
            function() { AgriTestUtils.validatePrice(-250.0); },
            /greater than (?:0|zero) RWF/i,
            'Negative price (-250.0 RWF) throws validation error.'
        );
    });

    QUnit.test('Test 4: Percentage deviation calculation formula', function(assert) {
        // Average = 500 RWF, New = 600 RWF -> Diff = 100 -> Dev = 100 / 500 * 100 = 20%
        const deviation = AgriTestUtils.calculateDeviation(600, 500);
        assert.strictEqual(deviation, 20.0, 'Deviation for 600 vs 500 is exactly 20.0%');

        // Symmetrical check: New = 400 RWF -> Dev = 100 / 500 * 100 = 20%
        const lowerDeviation = AgriTestUtils.calculateDeviation(400, 500);
        assert.strictEqual(lowerDeviation, 20.0, 'Deviation for 400 vs 500 is symmetrically 20.0%');
    });

    QUnit.test('Test 5: Outlier detection triggers on excessive price deviations (>25%)', function(assert) {
        const average = 500.0;
        const threshold = 25.0; // web.xml default
        
        // 650 RWF is +30% deviation (> 25% threshold)
        const isOutlierHigh = AgriTestUtils.isPriceOutlier(650.0, average, threshold);
        assert.true(isOutlierHigh, '650 RWF (+30%) flagged as outlier against 500 RWF average.');

        // 350 RWF is -30% deviation (> 25% threshold)
        const isOutlierLow = AgriTestUtils.isPriceOutlier(350.0, average, threshold);
        assert.true(isOutlierLow, '350 RWF (-30%) flagged as outlier against 500 RWF average.');
    });

    QUnit.test('Test 6: Boundary percentage behavior at threshold limit (25%)', function(assert) {
        const average = 400.0;
        const threshold = 25.0;
        
        // 500 RWF is exactly 25.0% deviation (should NOT exceed, so NOT an outlier)
        const isBoundary = AgriTestUtils.isPriceOutlier(500.0, average, threshold);
        assert.false(isBoundary, 'Exact 25.0% boundary price (500 RWF vs 400 RWF) is accepted.');

        // 501 RWF is 25.25% deviation (exceeds 25.0%, so IS an outlier)
        const isExceeded = AgriTestUtils.isPriceOutlier(501.0, average, threshold);
        assert.true(isExceeded, '501 RWF (25.25%) exceeds boundary and is rejected as outlier.');
    });

    QUnit.test('Test 7: Trailing 7-day average calculation across normal dataset', function(assert) {
        const history = [480, 490, 500, 510, 520, 510, 490]; // Sum = 3500, Length = 7
        const avg = AgriTestUtils.calculateAverage(history);
        assert.strictEqual(avg, 500.0, 'Average of 7 historical days is correctly calculated as 500.0 RWF');
    });

    QUnit.test('Test 8: Empty dataset handling returns 0.0 without division by zero', function(assert) {
        const empty = [];
        const avg = AgriTestUtils.calculateAverage(empty);
        assert.strictEqual(avg, 0.0, 'Empty price dataset safely returns 0.0 RWF');
    });

});

// =============================================================================
// MODULE 2: Watchlist Client-Side MVC & Submission Logic
// =============================================================================
QUnit.module('Module 2: Watchlist Client-Side MVC & Submission Rules', function() {

    QUnit.test('Test 9: WatchlistModel item addition & state preservation', function(assert) {
        const model = new WatchlistModel();
        model.setItems([]);
        
        const testItem = {
            watchlistId: 101,
            productId: 1,
            productName: 'Maize Grain',
            marketId: 1,
            marketName: 'Gisenyi Central Market',
            currentPrice: 520.0,
            targetPrice: 500.0
        };

        model.addItem(testItem);
        const stored = model.getItems();
        assert.strictEqual(stored.length, 1, 'WatchlistModel holds 1 item after addition.');
        assert.strictEqual(stored[0].productName, 'Maize Grain', 'Item data correctly preserved in state.');
    });

    QUnit.test('Test 10: Watchlist alert evaluates true when market price reaches target', function(assert) {
        const model = new WatchlistModel();
        
        // Scenario A: Market price is higher than target -> Alert FALSE
        model.setItems([{
            watchlistId: 1,
            productId: 1,
            currentPrice: 520.0,
            targetPrice: 500.0
        }]);
        assert.strictEqual(model.getAlertCount(), 0, 'No alert when market price (520) > target (500).');

        // Scenario B: Price drops to exactly target -> Alert TRUE
        model.setItems([{
            watchlistId: 1,
            productId: 1,
            currentPrice: 500.0,
            targetPrice: 500.0
        }]);
        assert.strictEqual(model.getAlertCount(), 1, 'Alert triggered when market price (500) == target (500).');

        // Scenario C: Price drops below target -> Alert TRUE
        model.setItems([{
            watchlistId: 1,
            productId: 1,
            currentPrice: 480.0,
            targetPrice: 500.0
        }]);
        assert.strictEqual(model.getAlertCount(), 1, 'Alert triggered when market price (480) < target (500).');
    });

    QUnit.test('Test 11: Missing or invalid product detection', function(assert) {
        function validateProductSelection(productId, availableProductIds) {
            if (!productId || !availableProductIds.includes(productId)) {
                throw new Error('UnknownProductException: Product does not exist in registry.');
            }
            return true;
        }

        const validProductIds = [1, 2, 3, 4, 5, 6];
        assert.ok(validateProductSelection(2, validProductIds), 'Product ID 2 exists in registry.');
        assert.throws(
            function() { validateProductSelection(999, validProductIds); },
            /UnknownProductException/,
            'Product ID 999 is detected as unknown and throws exception.'
        );
    });

    QUnit.test('Test 12: Duplicate daily submission logic prevention', function(assert) {
        const existingReports = [
            { marketId: 1, productId: 1, reportDate: '2026-11-09' },
            { marketId: 2, productId: 1, reportDate: '2026-11-09' }
        ];

        // Attempt duplicate: Market 1, Product 1 on 2026-11-09
        const isDupe = AgriTestUtils.isDuplicateSubmission(existingReports, 1, 1, '2026-11-09');
        assert.true(isDupe, 'Duplicate daily submission correctly detected and blocked.');

        // Attempt novel: Market 1, Product 2 on 2026-11-09
        const isNewProduct = AgriTestUtils.isDuplicateSubmission(existingReports, 1, 2, '2026-11-09');
        assert.false(isNewProduct, 'Distinct product in same market and day is permitted.');

        // Attempt novel: Market 1, Product 1 on 2026-11-10 (different day)
        const isNewDate = AgriTestUtils.isDuplicateSubmission(existingReports, 1, 1, '2026-11-10');
        assert.false(isNewDate, 'Same product and market on a new day is permitted.');
    });

});
