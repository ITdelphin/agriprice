/**
 * AgriPrice Rwanda - Main Application Controller
 * Handles preferences (cookies), live filtering, automatic polling,
 * agent session states, and UI interactions.
 */

// -----------------------------------------------------------------------------
// 1. Preference Cookie Management (Section 12 Compliance)
// -----------------------------------------------------------------------------
const AgriCookies = {
    set(name, value, days = 30) {
        const d = new Date();
        d.setTime(d.getTime() + (days * 24 * 60 * 60 * 1000));
        const expires = "expires=" + d.toUTCString();
        // Uses SameSite=Lax for secure cross-site protection while preserving top-level navigation
        document.cookie = `${name}=${encodeURIComponent(value)};${expires};path=/;SameSite=Lax`;
    },

    get(name) {
        const nameEQ = name + "=";
        const ca = document.cookie.split(';');
        for (let i = 0; i < ca.length; i++) {
            let c = ca[i];
            while (c.charAt(0) === ' ') c = c.substring(1);
            if (c.indexOf(nameEQ) === 0) {
                return decodeURIComponent(c.substring(nameEQ.length));
            }
        }
        return null;
    }
};

// -----------------------------------------------------------------------------
// 2. Main App Logic
// -----------------------------------------------------------------------------
const App = {
    currentAgent: null,
    pollIntervalId: null,

    async init() {
        this.applySavedPreferences();
        await this.checkAuth();
        this.setupNavigation();
        this.setupPreferencesUI();
    },

    // Check Agent Authentication State
    async checkAuth() {
        try {
            const auth = await AgriAPI.checkAuthStatus();
            const agentNav = document.getElementById('agentNavLinks');
            const guestNav = document.getElementById('guestNavLinks');
            const agentBadge = document.getElementById('agentIdentityBadge');

            if (auth && auth.authenticated) {
                this.currentAgent = auth;
                if (agentNav) agentNav.style.display = 'flex';
                if (guestNav) guestNav.style.display = 'none';
                if (agentBadge) {
                    agentBadge.textContent = `Agent: ${auth.fullName}`;
                    agentBadge.style.display = 'inline-block';
                }
            } else {
                this.currentAgent = null;
                if (agentNav) agentNav.style.display = 'none';
                if (guestNav) guestNav.style.display = 'flex';
                if (agentBadge) agentBadge.style.display = 'none';
            }
        } catch (e) {
            console.warn('Could not verify auth status:', e);
        }
    },

    // Read and Apply Saved Cookies (Market & Language)
    applySavedPreferences() {
        const preferredMarket = AgriCookies.get('preferred_market');
        const preferredLang = AgriCookies.get('preferred_language') || 'EN';

        const marketSelect = document.getElementById('filterMarket');
        if (marketSelect && preferredMarket) {
            marketSelect.value = preferredMarket;
        }

        const langSelect = document.getElementById('prefLanguage');
        if (langSelect) {
            langSelect.value = preferredLang;
        }
    },

    setupPreferencesUI() {
        const marketSelect = document.getElementById('prefMarket');
        if (marketSelect) {
            marketSelect.value = AgriCookies.get('preferred_market') || '1';
            marketSelect.addEventListener('change', (e) => {
                AgriCookies.set('preferred_market', e.target.value);
                this.showToast('Preferred market saved: ' + e.target.options[e.target.selectedIndex].text);
            });
        }

        const langSelect = document.getElementById('prefLanguage');
        if (langSelect) {
            langSelect.addEventListener('change', (e) => {
                AgriCookies.set('preferred_language', e.target.value);
                this.showToast('Language preference updated to ' + e.target.value);
            });
        }

        const logoutBtn = document.getElementById('btnLogout');
        if (logoutBtn) {
            logoutBtn.addEventListener('click', async (e) => {
                e.preventDefault();
                await AgriAPI.logout();
                this.showToast('Logged out successfully.');
                setTimeout(() => window.location.href = 'index.html', 800);
            });
        }
    },

    setupNavigation() {
        const links = document.querySelectorAll('.nav-link');
        const currentPath = window.location.pathname;
        links.forEach(link => {
            if (currentPath.endsWith(link.getAttribute('href'))) {
                link.classList.add('active');
            }
        });
    },

    // Toast Notification System
    showToast(message, type = 'success') {
        let container = document.getElementById('toastContainer');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toastContainer';
            container.className = 'toast-container';
            document.body.appendChild(container);
        }
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.textContent = message;
        container.appendChild(toast);
        setTimeout(() => toast.remove(), 4000);
    },

    marketPhoto(market) {
        const name = String((market && market.marketName) || '').toLowerCase();
        const photos = [
            { test: /gisenyi/, src: 'assets/images/markets/gisenyi.jpg', alt: 'Gisenyi town on the shore of Lake Kivu' },
            { test: /cross.?border/, src: 'assets/images/markets/rubavu.jpg', alt: 'Rubavu town centre near the DRC border' },
            { test: /mahoko/, src: 'assets/images/markets/mahoko.jpg', alt: 'Produce stalls at a Rwandan market (illustrative)' }
        ];
        const found = photos.find(item => item.test.test(name));
        if (found) return found;
        return {
            src: 'assets/images/kigali-market.jpg',
            alt: (market && market.marketName) ? `${market.marketName} market` : 'Rwandan market'
        };
    },

    // Automatic Price Polling every 5 seconds
    startAutoRefresh(callback) {
        if (this.pollIntervalId) clearInterval(this.pollIntervalId);
        this.pollIntervalId = setInterval(() => {
            if (!document.hidden && typeof callback === 'function') {
                callback();
            }
        }, 5000);
    },

    stopAutoRefresh() {
        if (this.pollIntervalId) {
            clearInterval(this.pollIntervalId);
            this.pollIntervalId = null;
        }
    }
};

document.addEventListener('DOMContentLoaded', () => {
    App.init();
});
