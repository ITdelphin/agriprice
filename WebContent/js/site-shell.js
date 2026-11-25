(function () {
    const siteLinks = [
        { href: 'index.html', label: 'Home' },
        { href: 'prices.html', label: 'Market Prices' },
        { href: 'products.html', label: 'Products' },
        { href: 'markets.html', label: 'Markets' },
        { href: 'trends.html', label: 'Price Trends' },
        { href: 'index.html#about', label: 'About Us' },
        { href: 'index.html#contact', label: 'Contact' }
    ];

    function currentPage() {
        const path = window.location.pathname.split('/').pop();
        return path && path.length ? path : 'index.html';
    }

    function navMarkup() {
        const links = siteLinks.map(link => {
            const active = currentPage() === link.href.split('#')[0] ? ' active' : '';
            return `<a class="nav-link${active}" href="${link.href}">${link.label}</a>`;
        }).join('');

        return `
<header class="site-navbar" id="siteNavbar">
    <div class="site-nav-inner">
        <a class="site-brand" href="index.html" aria-label="AgriPrice Rwanda home">
            <img src="assets/images/logo.svg" alt="" class="site-brand-logo">
            <span>AgriPrice <strong>Rwanda</strong></span>
        </a>
        <button class="site-nav-toggle" id="siteNavToggle" type="button" aria-expanded="false" aria-controls="siteNavLinks">Menu</button>
        <nav class="site-nav-links" id="siteNavLinks" aria-label="Main navigation">
            ${links}
            <span class="site-auth" data-auth-role="guest"><a class="btn btn-secondary btn-sm" href="login.html">Agent Login</a></span>
            <span class="site-auth" data-auth-role="agent" hidden><a class="btn btn-outline btn-sm" href="dashboard.html">Dashboard</a></span>
            <span class="site-auth" data-auth-role="agent" hidden><a class="btn btn-danger btn-sm" id="siteLogoutBtn" href="#">Sign out</a></span>
            <a class="btn btn-primary btn-sm" href="prices.html#watchlist" id="watchlistBadge" aria-label="Open watchlist">Watchlist</a>
        </nav>
    </div>
</header>
<div class="site-backbar" id="siteBackbar">
    <div class="site-backbar-inner">
        <button class="back-button" id="siteBackButton" type="button">← Back</button>
        <span class="site-page-label" id="sitePageLabel"></span>
    </div>
</div>`;
    }

    function footerMarkup() {
        const year = new Date().getFullYear();
        return `
<footer class="site-footer" id="siteFooter">
    <div class="site-footer-inner">
        <div class="site-footer-brand">
            <img src="assets/images/logo.svg" alt="" class="site-footer-logo">
            <div>
                <strong>AgriPrice Rwanda</strong>
                <p>Clear daily produce prices from Rwandan markets, shared for farmers, traders, and buyers.</p>
            </div>
        </div>
        <div class="site-footer-links">
            <h3>Explore</h3>
            <a href="index.html">Home</a>
            <a href="prices.html">Market Prices</a>
            <a href="products.html">Products</a>
            <a href="markets.html">Markets</a>
            <a href="trends.html">Price Trends</a>
        </div>
        <div class="site-footer-links" id="contact">
            <h3>Contact</h3>
            <p>For market data questions, contact the AgriPrice Rwanda team through your local market desk.</p>
            <a href="login.html">Agent access</a>
        </div>
    </div>
    <div class="site-footer-bottom">© ${year} AgriPrice Rwanda. Built for Rwandan markets.</div>
</footer>`;
    }

    function removeLegacyChrome() {
        const selectors = [
            'header.navbar',
            'header.public-header',
            '.catalog-topbar',
            '.agent-topbar',
            '.login-header-nav',
            'footer.footer',
            'footer.home2-footer',
            'footer.login-footer',
            'footer.catalog-footer'
        ];
        selectors.forEach(selector => {
            document.querySelectorAll(selector).forEach(node => node.remove());
        });
    }

    function setupNav() {
        const toggle = document.getElementById('siteNavToggle');
        const links = document.getElementById('siteNavLinks');
        if (!toggle || !links) return;

        toggle.addEventListener('click', () => {
            const open = links.classList.toggle('open');
            toggle.setAttribute('aria-expanded', String(open));
        });

        links.addEventListener('click', event => {
            if (event.target.closest('a')) {
                links.classList.remove('open');
                toggle.setAttribute('aria-expanded', 'false');
            }
        });
    }

    function setupBack() {
        const button = document.getElementById('siteBackButton');
        const label = document.getElementById('sitePageLabel');
        if (!button) return;

        if (currentPage() === 'index.html') {
            document.getElementById('siteBackbar').hidden = true;
            return;
        }

        if (label) {
            const active = document.querySelector('.site-nav-links .nav-link.active');
            label.textContent = active ? active.textContent.trim() : document.title.split('|')[0].trim();
        }

        button.addEventListener('click', () => {
            let sameOrigin = false;
            try {
                sameOrigin = !!document.referrer && new URL(document.referrer).origin === window.location.origin;
            } catch (error) {
                sameOrigin = false;
            }
            if (sameOrigin && window.history.length > 1) {
                window.history.back();
            } else {
                window.location.href = 'prices.html';
            }
        });
    }

    async function setupAuth() {
        let isAgent = false;
        try {
            const auth = await AgriAPI.checkAuthStatus();
            isAgent = !!(auth && auth.authenticated);
        } catch (error) {
            isAgent = false;
        }

        document.querySelectorAll('[data-auth-role]').forEach(node => {
            node.hidden = node.dataset.authRole !== (isAgent ? 'agent' : 'guest');
        });

        const logout = document.getElementById('siteLogoutBtn');
        if (logout) {
            logout.addEventListener('click', async event => {
                event.preventDefault();
                await AgriAPI.logout();
                window.location.href = 'index.html';
            });
        }
    }

    function mount() {
        if (document.body.dataset.siteShellMounted === 'true') return;
        document.body.dataset.siteShellMounted = 'true';
        document.body.classList.add('has-site-shell');
        removeLegacyChrome();
        document.body.insertAdjacentHTML('afterbegin', navMarkup());
        document.body.insertAdjacentHTML('beforeend', footerMarkup());
        setupNav();
        setupBack();
        setupAuth();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', mount, { once: true });
    } else {
        mount();
    }
})();
