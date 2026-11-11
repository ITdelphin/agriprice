/**
 * AgriPrice Rwanda - Watchlist Client-Side MVC: MODEL
 * Strict Requirement: State management and calculations ONLY. No DOM manipulation.
 */
class WatchlistModel {
    constructor() {
        this.items = [];
        this.visitorSessionId = this.loadVisitorSessionId();
        this.alertCount = 0;
        this.listeners = [];
    }

    loadVisitorSessionId() {
        // Read cookie or local cache
        const match = document.cookie.match(new RegExp('(^| )agriprice_visitor=([^;]+)'));
        if (match) return match[2];
        const cached = localStorage.getItem('agriprice_visitor_id');
        if (cached) return cached;
        const generated = 'visitor-' + Math.random().toString(36).substring(2, 12);
        localStorage.setItem('agriprice_visitor_id', generated);
        return generated;
    }

    setVisitorSessionId(id) {
        this.visitorSessionId = id;
        localStorage.setItem('agriprice_visitor_id', id);
    }

    setItems(items) {
        this.items = items || [];
        this.evaluateAlerts();
        this.notifyListeners();
    }

    getItems() {
        return [...this.items];
    }

    getItem(watchlistId) {
        return this.items.find(item => item.watchlistId === watchlistId);
    }

    addItem(item) {
        const existingIdx = this.items.findIndex(i => i.productId === item.productId && i.marketId === item.marketId);
        if (existingIdx >= 0) {
            this.items[existingIdx] = item;
        } else {
            this.items.unshift(item);
        }
        this.evaluateAlerts();
        this.notifyListeners();
    }

    updateTargetPrice(watchlistId, newTargetPrice) {
        const item = this.getItem(watchlistId);
        if (item) {
            item.targetPrice = parseFloat(newTargetPrice);
            this.evaluateAlerts();
            this.notifyListeners();
        }
    }

    removeItem(watchlistId) {
        this.items = this.items.filter(item => item.watchlistId !== watchlistId);
        this.evaluateAlerts();
        this.notifyListeners();
    }

    evaluateAlerts() {
        let count = 0;
        this.items.forEach(item => {
            if (item.currentPrice > 0 && item.currentPrice <= item.targetPrice) {
                item.targetReached = true;
                count++;
            } else {
                item.targetReached = false;
            }
        });
        this.alertCount = count;
    }

    getAlertCount() {
        return this.alertCount;
    }

    subscribe(listener) {
        this.listeners.push(listener);
    }

    notifyListeners() {
        this.listeners.forEach(fn => fn(this.items, this.alertCount));
    }
}

// Export for browser and QUnit
if (typeof module !== 'undefined' && module.exports) {
    module.exports = WatchlistModel;
}
