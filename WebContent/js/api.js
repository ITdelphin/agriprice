/**
 * AgriPrice Rwanda - API Client Layer
 * Encapsulates Fetch API calls and demonstrates an explicit XMLHttpRequest call
 * as mandated by assessment specification Section 13.
 */
const AgriAPI = {
    // 1. Fetch Prices with Optional Filters
    async getPrices(params = {}) {
        const query = new URLSearchParams(params).toString();
        const url = `api/prices${query ? '?' + query : ''}`;
        const res = await fetch(url);
        if (!res.ok) throw new Error(`HTTP error ${res.status}`);
        return await res.json();
    },

    // 2. Fetch All Markets
    async getMarkets() {
        const res = await fetch('api/markets');
        if (!res.ok) throw new Error(`HTTP error ${res.status}`);
        return await res.json();
    },

    // 3. Fetch All Products
    async getProducts() {
        const res = await fetch('api/products');
        if (!res.ok) throw new Error(`HTTP error ${res.status}`);
        return await res.json();
    },

    // 4. Fetch Dashboard Aggregate Statistics
    async getDashboardStats() {
        const res = await fetch('api/dashboard');
        if (!res.ok) throw new Error(`HTTP error ${res.status}`);
        return await res.json();
    },

    // 5. Submit Price Report (Agent authenticated POST)
    async submitPrice(data) {
        const res = await fetch('api/prices', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        const result = await res.json();
        return { status: res.status, data: result };
    },

    // 6. Update Price Report
    async updatePrice(data) {
        data.action = 'update';
        const res = await fetch('api/prices', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        return await res.json();
    },

    // 7. Delete Price Report
    async deletePrice(reportId) {
        const res = await fetch('api/prices', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: 'delete', reportId })
        });
        return await res.json();
    },

    // 8. Auth Operations
    async login(email, password) {
        const res = await fetch('api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        const data = await res.json();
        return { status: res.status, data };
    },

    async logout() {
        const res = await fetch('api/auth/logout', { method: 'POST' });
        return await res.json();
    },

    async checkAuthStatus() {
        const res = await fetch('api/auth/status');
        return await res.json();
    },

    // 9. Trend Data for Visualizations
    async getTrendHistory(productId, marketId, days = 14) {
        const url = `api/trends/history?productId=${productId}&marketId=${marketId || ''}&days=${days}`;
        const res = await fetch(url);
        return await res.json();
    },

    async getMarketComparison(productId, date) {
        const url = `api/trends/comparison?productId=${productId}&date=${date || '2026-11-09'}`;
        const res = await fetch(url);
        return await res.json();
    },

    async getMarketDistribution() {
        const res = await fetch('api/trends/distribution');
        return await res.json();
    },

    /**
     * MANDATORY ASSESSMENT REQUIREMENT (Section 13):
     * Implements functional communication using legacy XMLHttpRequest (XHR)
     * to satisfy the specific examination requirement.
     * Fetches quick market snapshot asynchronously using XMLHttpRequest.
     */
    fetchMarketSnapshotXHR(callback, errorCallback) {
        const xhr = new XMLHttpRequest();
        xhr.open('GET', 'api/dashboard', true);
        xhr.setRequestHeader('Accept', 'application/json');
        xhr.onreadystatechange = function() {
            if (xhr.readyState === 4) {
                if (xhr.status >= 200 && xhr.status < 300) {
                    try {
                        const json = JSON.parse(xhr.responseText);
                        callback(json);
                    } catch (e) {
                        if (errorCallback) errorCallback('Failed to parse XHR JSON response');
                    }
                } else {
                    if (errorCallback) errorCallback('XHR request failed with HTTP ' + xhr.status);
                }
            }
        };
        xhr.onerror = function() {
            if (errorCallback) errorCallback('XHR Network error');
        };
        xhr.send();
    }
};
