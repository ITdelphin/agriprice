/* Shared responsive navigation for the AgriPrice workspace pages. */
(function () {
    const links = [
        { href: 'dashboard.html', label: 'Dashboard', icon: '⌂' },
        { href: 'submit-price.html', label: 'Submit price', icon: '+' },
        { href: 'products.html', label: 'Manage products', icon: '▦' },
        { href: 'markets.html', label: 'Manage markets', icon: '⌖' },
        { href: 'prices.html', label: 'Price reports', icon: '≋' },
        { href: 'watchlist.html', label: 'Watchlist', icon: '☆' },
        { href: 'trends.html', label: 'Price trends', icon: '⌁' },
        { href: 'export.html', label: 'XML export', icon: '〈/〉' }
    ];

    function mount() {
        const body = document.body;
        if (body.dataset.appShell === 'off') return;
        body.classList.add('app-layout');
        const activePage = location.pathname.split('/').pop() || 'index.html';
        const nav = links.map(item => `<a class="side-link${activePage === item.href ? ' is-active' : ''}" href="${item.href}" aria-label="${item.label}" title="${item.label}"${activePage === item.href ? ' aria-current="page"' : ''}><span class="side-icon" aria-hidden="true">${item.icon}</span><span class="side-label">${item.label}</span></a>`).join('');
        body.insertAdjacentHTML('afterbegin', `
            <button class="sidebar-mobile-toggle" id="sidebarMobileToggle" type="button" aria-label="Open navigation" aria-expanded="false">☰</button>
            <div class="sidebar-backdrop" id="sidebarBackdrop"></div>
            <aside class="app-sidebar" id="appSidebar" aria-label="Application navigation">
                <a class="side-brand" href="index.html" aria-label="AgriPrice Rwanda home"><span class="side-brand-mark" aria-hidden="true">✳</span><span class="side-label"><strong>AgriPrice</strong><small>RWANDA MARKET INTELLIGENCE</small></span></a>
                <div class="side-section-label">WORKSPACE</div>
                <nav class="side-nav">${nav}</nav>
                <div class="side-bottom">
                    <a class="side-link${activePage === 'settings.html' ? ' is-active' : ''}" href="settings.html" aria-label="Settings" title="Settings"${activePage === 'settings.html' ? ' aria-current="page"' : ''}><span class="side-icon" aria-hidden="true">⚙</span><span class="side-label">Settings</span></a>
                    <div class="side-profile" id="sideProfile"><span class="side-avatar" aria-hidden="true">•</span><span class="side-label"><strong id="sideAgentName">Market workspace</strong><small id="sideAgentEmail">Public market data</small></span></div>
                    <button class="side-link side-auth-action" id="sideAuthAction" type="button" aria-label="Sign in" title="Sign in"><span class="side-icon" aria-hidden="true">↪</span><span class="side-label">Sign in</span></button>
                </div>
                <button class="sidebar-collapse" id="sidebarCollapse" type="button" aria-label="Collapse navigation">‹</button>
            </aside>`);

        const sidebar = document.getElementById('appSidebar');
        const toggle = document.getElementById('sidebarMobileToggle');
        const backdrop = document.getElementById('sidebarBackdrop');
        toggle.addEventListener('click', () => {
            const open = body.classList.toggle('sidebar-open');
            toggle.setAttribute('aria-expanded', String(open));
            toggle.setAttribute('aria-label', open ? 'Close navigation' : 'Open navigation');
        });
        const closeMobileNav = () => {
            body.classList.remove('sidebar-open');
            toggle.setAttribute('aria-expanded', 'false');
            toggle.setAttribute('aria-label', 'Open navigation');
        };
        backdrop.addEventListener('click', closeMobileNav);
        document.getElementById('sidebarCollapse').addEventListener('click', () => {
            const collapsed = body.classList.toggle('sidebar-collapsed');
            document.getElementById('sidebarCollapse').textContent = collapsed ? '›' : '‹';
            document.getElementById('sidebarCollapse').setAttribute('aria-label', collapsed ? 'Expand navigation' : 'Collapse navigation');
        });
        document.addEventListener('keydown', event => {
            if (event.key === 'Escape') closeMobileNav();
        });

        const action = document.getElementById('sideAuthAction');
        action.addEventListener('click', async () => {
            if (action.dataset.authenticated === 'true') {
                try { await AgriAPI.logout(); } finally { location.href = 'login.html'; }
            } else {
                location.href = 'login.html';
            }
        });
        if (window.AgriAPI) {
            AgriAPI.checkAuthStatus().then(auth => {
                if (!auth || !auth.authenticated) return;
                document.getElementById('sideAgentName').textContent = auth.fullName || 'Market agent';
                document.getElementById('sideAgentEmail').textContent = auth.email || 'Signed in';
                action.dataset.authenticated = 'true';
                action.setAttribute('aria-label', 'Sign out');
                action.title = 'Sign out';
                action.querySelector('.side-label').textContent = 'Sign out';
            }).catch(() => {});
        }
    }

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', mount);
    else mount();
})();
