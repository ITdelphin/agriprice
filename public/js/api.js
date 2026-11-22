const STATIC_HOSTED = new URLSearchParams(location.search).has('static');

const AgriAPI = {
    _cache: {},

    async _staticJson(name) {
        if (!this._cache[name]) {
            const res = await fetch(`data/${name}.json`);
            if (!res.ok) throw new Error(`Static data unavailable: ${name}`);
            this._cache[name] = await res.json();
        }
        return this._cache[name];
    },

    async getPrices(params = {}) {
        if (!STATIC_HOSTED) {
            const query = new URLSearchParams(params).toString();
            const url = `api/prices${query ? '?' + query : ''}`;
            const res = await fetch(url);
            if (!res.ok) throw new Error(`HTTP error ${res.status}`);
            return await res.json();
        }
        const all = params.all === true || params.all === 'true';
        let reports = await this._staticJson('prices');
        if (all) return reports;
        const productId = params.productId ? Number(params.productId) : null;
        const marketId = params.marketId ? Number(params.marketId) : null;
        const date = params.date || '';
        const search = String(params.search || '').toLowerCase();
        if (productId || marketId || date || search) {
            reports = reports.filter(report => {
                if (productId && report.productId !== productId) return false;
                if (marketId && report.marketId !== marketId) return false;
                if (date && report.reportDate !== date) return false;
                if (search) {
                    const haystack = `${report.productName} ${report.marketName} ${report.marketDistrict}`.toLowerCase();
                    if (!haystack.includes(search)) return false;
                }
                return true;
            });
        }
        return reports;
    },

    async getMarkets() {
        if (!STATIC_HOSTED) {
            const res = await fetch('api/markets');
            if (!res.ok) throw new Error(`HTTP error ${res.status}`);
            return await res.json();
        }
        return await this._staticJson('markets');
    },

    async getProducts() {
        if (!STATIC_HOSTED) {
            const res = await fetch('api/products');
            if (!res.ok) throw new Error(`HTTP error ${res.status}`);
            return await res.json();
        }
        return await this._staticJson('products');
    },

    async getDashboardStats() {
        if (!STATIC_HOSTED) {
            const res = await fetch('api/dashboard');
            if (!res.ok) throw new Error(`HTTP error ${res.status}`);
            return await res.json();
        }
        return await this._staticJson('dashboard');
    },

    async getAgentPrices() {
        if (!STATIC_HOSTED) {
            const res = await fetch('api/prices/agent', { headers: { Accept: 'application/json' } });
            if (!res.ok) throw new Error(`Agent price request failed (${res.status}).`);
            return await res.json();
        }
        return [];
    },

    async submitPrice(data) {
        if (STATIC_HOSTED) {
            return { status: 403, data: { success: false, message: 'This hosted demo is read-only.' } };
        }
        const res = await fetch('api/prices', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
            body: JSON.stringify(data)
        });
        const text = await res.text();
        let result;
        try {
            result = text ? JSON.parse(text) : { success: false, message: 'Empty response from server.' };
        } catch (_) {
            throw new Error(`Server returned an invalid response (HTTP ${res.status}).`);
        }
        return { status: res.status, data: result };
    },

    async updatePrice(data) {
        if (STATIC_HOSTED) return { success: false, message: 'This hosted demo is read-only.' };
        data.action = 'update';
        const res = await fetch('api/prices', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        return await res.json();
    },

    async deletePrice(reportId) {
        if (STATIC_HOSTED) return { success: false, message: 'This hosted demo is read-only.' };
        const res = await fetch('api/prices', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: 'delete', reportId })
        });
        return await res.json();
    },

    async login(email, password) {
        if (STATIC_HOSTED) {
            return { status: 403, data: { success: false, message: 'Sign-in is disabled in the hosted demo.' } };
        }
        const res = await fetch('api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        const data = await res.json();
        return { status: res.status, data };
    },

    async logout() {
        if (STATIC_HOSTED) return { success: true, message: 'Logged out successfully' };
        const res = await fetch('api/auth/logout', { method: 'POST' });
        return await res.json();
    },

    async requestPasswordReset(email) {
        if (STATIC_HOSTED) {
            return { success: false, message: 'Password reset is disabled in the static demo.' };
        }
        const res = await fetch('api/auth/forgot-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
            body: JSON.stringify({ email })
        });
        return await res.json();
    },

    async resetPassword(token, newPassword) {
        if (STATIC_HOSTED) {
            return { success: false, message: 'Password reset is disabled in the static demo.' };
        }
        const res = await fetch('api/auth/reset-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
            body: JSON.stringify({ token, newPassword })
        });
        return await res.json();
    },

    async checkAuthStatus() {
        if (STATIC_HOSTED) return { authenticated: false };
        const res = await fetch('api/auth/status');
        return await res.json();
    },

    async getTrendHistory(productId, marketId, days = 14) {
        if (!STATIC_HOSTED) {
            const url = `api/trends/history?productId=${productId}&marketId=${marketId || ''}&days=${days}`;
            const res = await fetch(url);
            if (!res.ok) throw new Error(`Trend request failed (${res.status}).`);
            return await res.json();
        }
        const reports = (await this._staticJson('prices')).filter(report =>
            (!productId || report.productId === Number(productId)) &&
            (!marketId || report.marketId === Number(marketId))
        );
        const groups = {};
        for (const report of reports) {
            if (!groups[report.reportDate]) groups[report.reportDate] = [];
            groups[report.reportDate].push(Number(report.priceRwf));
        }
        const limit = Math.max(1, Number(days) || 14);
        return Object.keys(groups).sort().slice(-limit).map(date => {
            const values = groups[date];
            return {
                date,
                avgPrice: values.reduce((sum, value) => sum + value, 0) / values.length,
                minPrice: Math.min(...values),
                maxPrice: Math.max(...values)
            };
        });
    },

    async getMarketComparison(productId, date) {
        if (!STATIC_HOSTED) {
            const url = `api/trends/comparison?productId=${encodeURIComponent(productId)}${date ? '&date=' + encodeURIComponent(date) : ''}`;
            const res = await fetch(url);
            if (!res.ok) throw new Error(`Comparison request failed (${res.status}).`);
            return await res.json();
        }
        const reports = await this._staticJson('prices');
        const target = reports.filter(report =>
            report.productId === Number(productId) && (!date || report.reportDate === date)
        );
        const latestDate = date || target.reduce((max, report) => report.reportDate > max ? report.reportDate : max, '');
        return reports
            .filter(report => report.productId === Number(productId) && report.reportDate === latestDate)
            .map(report => ({
                marketId: report.marketId,
                marketName: report.marketName,
                district: report.marketDistrict,
                price: Number(report.priceRwf),
                unit: report.unit
            }))
            .sort((a, b) => a.price - b.price);
    },

    async getMarketDistribution() {
        if (!STATIC_HOSTED) {
            const res = await fetch('api/trends/distribution');
            if (!res.ok) throw new Error(`Distribution request failed (${res.status}).`);
            return await res.json();
        }
        const dashboard = await this._staticJson('dashboard');
        return dashboard.marketDistribution;
    },

    fetchMarketSnapshotXHR(callback, errorCallback) {
        const xhr = new XMLHttpRequest();
        const url = STATIC_HOSTED ? 'data/dashboard.json' : 'api/dashboard';
        xhr.open('GET', url, true);
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
