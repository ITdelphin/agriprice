/* Product catalog page: AJAX data, filters, paging and agent-only CRUD. */
(function () {
    'use strict';

    const pageSize = 8;
    const state = { products: [], filtered: [], page: 1, isAgent: false, currentUser: null, loading: false };
    const byId = id => document.getElementById(id);

    document.addEventListener('DOMContentLoaded', init);

    async function init() {
        byId('catalogDate').textContent = new Intl.DateTimeFormat('en-RW', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' }).format(new Date());
        bindNavigation();
        bindFilters();
        bindModal();
        try {
            const auth = await AgriAPI.checkAuthStatus();
            state.isAgent = Boolean(auth && auth.authenticated);
            state.currentUser = auth && auth.authenticated ? auth : null;
            renderProfile();
        } catch (_) {
            state.isAgent = false;
            renderProfile();
        }
        await loadProducts();
    }

    function bindNavigation() {
        document.addEventListener('keydown', event => { if (event.key === 'Escape') closeModal(); });

        const notificationButton = byId('catalogNotifications');
        const notificationPanel = byId('catalogNotice');
        const userButton = byId('catalogUserMenuButton');
        const userPanel = byId('catalogUserMenu');
        notificationButton.addEventListener('click', () => togglePopover(notificationButton, notificationPanel));
        userButton.addEventListener('click', () => togglePopover(userButton, userPanel));
        document.addEventListener('click', event => {
            if (!event.target.closest('.catalog-menu-wrap')) {
                closePopover(notificationButton, notificationPanel);
                closePopover(userButton, userPanel);
            }
        });
        byId('catalogSignOut').addEventListener('click', signInOrOut);
        const globalSearch = byId('globalProductSearch');
        globalSearch.addEventListener('focus', () => globalSearch.closest('.catalog-top-search').classList.add('is-open'));
        globalSearch.closest('.catalog-top-search').addEventListener('click', event => {
            if (window.matchMedia('(max-width: 700px)').matches) {
                event.currentTarget.classList.add('is-open');
                globalSearch.focus();
            }
        });
    }

    function togglePopover(button, panel) {
        const show = panel.hidden;
        panel.hidden = !show;
        button.setAttribute('aria-expanded', String(show));
    }
    function closePopover(button, panel) { panel.hidden = true; button.setAttribute('aria-expanded', 'false'); }

    async function signInOrOut() {
        if (state.currentUser) await signOut();
        else location.href = 'login.html';
    }

    async function signOut() {
        try { await AgriAPI.logout(); } catch (_) { /* Return to sign-in even if the session has expired. */ }
        location.href = 'login.html';
    }

    function renderProfile() {
        const name = state.currentUser ? (state.currentUser.fullName || 'Market agent') : 'Guest visitor';
        const role = state.currentUser ? 'Market agent' : 'Public access';
        const initial = name.trim().charAt(0).toUpperCase() || 'G';
        byId('topUserName').textContent = name;
        byId('topUserRole').textContent = role;
        byId('topAvatar').textContent = initial;
        byId('catalogSignOut').textContent = state.currentUser ? 'Sign out' : 'Agent sign in';
        byId('catalogDashboardLink').hidden = !state.currentUser;
        byId('btnOpenAddProduct').hidden = false;
        byId('catalogNotifications').setAttribute('aria-label', state.currentUser ? 'Catalog notifications' : 'Sign in to receive catalog notifications');
    }

    function bindFilters() {
        byId('productFilterForm').addEventListener('submit', event => { event.preventDefault(); applyFilters(); });
        byId('productSearchInput').addEventListener('input', applyFilters);
        byId('productCategoryFilter').addEventListener('change', applyFilters);
        byId('productStatusFilter').addEventListener('change', applyFilters);
        byId('globalProductSearch').addEventListener('input', event => {
            byId('productSearchInput').value = event.target.value;
            applyFilters();
        });
        byId('globalProductSearch').addEventListener('keydown', event => {
            if (event.key === 'Enter') { event.preventDefault(); byId('productSearchInput').focus(); }
        });
        byId('clearProductFilters').addEventListener('click', () => {
            byId('productFilterForm').reset();
            byId('globalProductSearch').value = '';
            applyFilters();
        });
        byId('productPrevPage').addEventListener('click', () => changePage(state.page - 1));
        byId('productNextPage').addEventListener('click', () => changePage(state.page + 1));
        byId('productPageNumbers').addEventListener('click', event => {
            const button = event.target.closest('[data-page]');
            if (button) changePage(Number(button.dataset.page));
        });
        byId('productsTableBody').addEventListener('click', handleTableAction);
    }

    async function loadProducts() {
        state.loading = true;
        renderLoading();
        try {
            const products = await AgriAPI.getProducts();
            if (!Array.isArray(products)) throw new Error('The product service returned an unexpected response.');
            state.products = products;
            populateCategories();
            state.loading = false;
            applyFilters(false);
        } catch (error) {
            state.loading = false;
            renderError(error.message || 'Products could not be loaded.');
        }
    }

    function populateCategories() {
        const select = byId('productCategoryFilter');
        const selected = select.value;
        const categories = [...new Set(state.products.map(product => product.category).filter(Boolean))].sort((a, b) => a.localeCompare(b));
        select.innerHTML = '<option value="">All categories</option>' + categories.map(category => `<option value="${escapeHtml(category)}">${escapeHtml(category)}</option>`).join('');
        if (categories.includes(selected)) select.value = selected;
        const datalist = byId('productCategoryOptions');
        datalist.innerHTML = categories.map(category => `<option value="${escapeHtml(category)}"></option>`).join('') + '<option value="Cereals"></option><option value="Legumes"></option><option value="Tubers"></option><option value="Vegetables"></option><option value="Fruits"></option>';
    }

    function applyFilters(resetPage = true) {
        if (resetPage) state.page = 1;
        const term = byId('productSearchInput').value.trim().toLocaleLowerCase();
        const category = byId('productCategoryFilter').value;
        const status = byId('productStatusFilter').value;
        state.filtered = state.products.filter(product => {
            const name = String(product.productName || '').toLocaleLowerCase();
            const description = String(product.description || '').toLocaleLowerCase();
            return (!term || name.includes(term) || description.includes(term))
                && (!category || product.category === category)
                && (!status || product.status === status);
        });
        const pages = Math.max(1, Math.ceil(state.filtered.length / pageSize));
        state.page = Math.min(state.page, pages);
        renderProducts();
    }

    function renderProducts() {
        const tbody = byId('productsTableBody');
        const total = state.filtered.length;
        const pages = Math.max(1, Math.ceil(total / pageSize));
        const startIndex = (state.page - 1) * pageSize;
        const visible = state.filtered.slice(startIndex, startIndex + pageSize);
        byId('productsCount').textContent = `${total} ${total === 1 ? 'product' : 'products'}`;
        byId('catalogHeroCount').textContent = `${state.products.length} products in the directory`;
        byId('catalogResultNote').textContent = total ? `Showing ${startIndex + 1}–${Math.min(startIndex + pageSize, total)} of ${total}` : '';
        if (!visible.length) {
            tbody.innerHTML = `<tr class="catalog-state-row"><td colspan="8"><div class="catalog-state catalog-empty-state"><span aria-hidden="true">—</span><strong>${state.products.length ? 'No products match these filters' : 'The product catalog is empty'}</strong><small>${state.products.length ? 'Try a different name, category or status.' : 'Products added by agents will appear here.'}</small>${state.products.length ? '<button type="button" id="emptyClearFilters">Clear filters</button>' : ''}</div></td></tr>`;
            byId('productPaginationInfo').textContent = 'No products to show';
            renderPageControls(pages);
            const clear = byId('emptyClearFilters');
            if (clear) clear.addEventListener('click', () => byId('clearProductFilters').click());
            return;
        }
        tbody.innerHTML = visible.map((product, index) => renderRow(product, startIndex + index + 1)).join('');
        byId('productPaginationInfo').textContent = `Showing ${startIndex + 1}–${Math.min(startIndex + pageSize, total)} of ${total} products`;
        renderPageControls(pages);
    }

    function renderRow(product, rowNumber) {
        const id = Number(product.productId) || 0;
        const name = String(product.productName || 'Unnamed product');
        const status = String(product.status || 'INACTIVE').toUpperCase();
        const actionMarkup = state.isAgent
            ? `<div class="catalog-row-actions"><button type="button" class="catalog-edit-button" data-action="edit" data-product-id="${id}">Edit</button><button type="button" class="catalog-delete-button" data-action="delete" data-product-id="${id}">Delete</button></div>`
            : `<a class="catalog-view-button" href="prices.html?productId=${encodeURIComponent(id)}">View prices</a>`;
        return `<tr>
            <td data-label="No."><span class="catalog-product-number">${String(product.productId || rowNumber).padStart(3, '0')}</span></td>
            <td data-label="Product name"><strong class="catalog-product-name">${escapeHtml(name)}</strong><small class="catalog-product-id">PRD-${String(id).padStart(4, '0')}</small></td>
            <td data-label="Category"><span class="catalog-category-pill">${escapeHtml(product.category || 'Uncategorized')}</span></td>
            <td data-label="Standard unit"><span class="catalog-unit">${escapeHtml(product.defaultUnit || '—')}</span></td>
            <td data-label="Description"><span class="catalog-description">${escapeHtml(product.description || 'No description provided')}</span></td>
            <td data-label="Status"><span class="catalog-status catalog-status-${status.toLowerCase()}"><i></i>${escapeHtml(status)}</span></td>
            <td data-label="Actions">${actionMarkup}</td>
        </tr>`;
    }


    function renderPageControls(totalPages) {
        const controls = byId('productPageNumbers');
        const pageWindow = Math.min(totalPages, 5);
        let first = Math.max(1, state.page - 2);
        first = Math.min(first, Math.max(1, totalPages - pageWindow + 1));
        controls.innerHTML = Array.from({ length: pageWindow }, (_, index) => {
            const page = first + index;
            return `<button type="button" data-page="${page}" class="${page === state.page ? 'is-current' : ''}" aria-label="Page ${page}" aria-current="${page === state.page ? 'page' : 'false'}">${page}</button>`;
        }).join('');
        byId('productPrevPage').disabled = state.page <= 1;
        byId('productNextPage').disabled = state.page >= totalPages;
    }

    function changePage(page) {
        const totalPages = Math.max(1, Math.ceil(state.filtered.length / pageSize));
        if (page < 1 || page > totalPages || page === state.page) return;
        state.page = page;
        renderProducts();
        byId('productTableTitle').scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    function renderLoading() {
        byId('productsTableBody').innerHTML = '<tr class="catalog-state-row"><td colspan="8"><div class="catalog-state"><span class="catalog-spinner" aria-hidden="true"></span><strong>Loading product catalog</strong><small>Connecting to the market database…</small></div></td></tr>';
        byId('productsCount').textContent = 'Loading…';
        byId('catalogHeroCount').textContent = 'Loading product catalog';
    }

    function renderError(message) {
        byId('productsTableBody').innerHTML = `<tr class="catalog-state-row"><td colspan="8"><div class="catalog-state catalog-error-state"><strong>Product catalog unavailable</strong><small>${escapeHtml(message)}</small><button type="button" id="retryProductLoad">Try again</button></div></td></tr>`;
        byId('productsCount').textContent = 'Unable to load';
        byId('catalogHeroCount').textContent = 'Catalog connection unavailable';
        byId('retryProductLoad').addEventListener('click', loadProducts);
    }

    async function handleTableAction(event) {
        const button = event.target.closest('[data-action][data-product-id]');
        if (!button || !state.isAgent) return;
        const product = state.products.find(item => Number(item.productId) === Number(button.dataset.productId));
        if (!product) return;
        if (button.dataset.action === 'edit') openEditModal(product);
        if (button.dataset.action === 'delete') await deleteProduct(product, button);
    }

    async function deleteProduct(product, button) {
        const message = `Delete “${product.productName}”? If price reports reference it, the product will be archived and its history will be preserved.`;
        if (!window.confirm(message)) return;
        button.disabled = true;
        try {
            const response = await fetch('api/products', {
                method: 'POST', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: 'delete', productId: Number(product.productId) })
            });
            const result = await response.json();
            if (!response.ok || !result.success) throw new Error(result.message || 'Product could not be removed.');
            showToast(result.message || 'Product removed from the catalog.', 'success');
            await loadProducts();
        } catch (error) {
            showToast(error.message || 'Product could not be removed. Try again.', 'error');
            button.disabled = false;
        }
    }

    function bindModal() {
        byId('btnOpenAddProduct').addEventListener('click', () => {
            if (state.isAgent) openCreateModal();
            else location.href = 'login.html';
        });
        byId('btnCloseProductModal').addEventListener('click', closeModal);
        byId('btnCancelProductModal').addEventListener('click', closeModal);
        byId('productModal').addEventListener('click', event => { if (event.target === byId('productModal')) closeModal(); });
        byId('productForm').addEventListener('submit', saveProduct);
    }

    function openCreateModal() {
        if (!state.isAgent) return;
        byId('productForm').reset();
        byId('modalAction').value = 'create';
        byId('modalProductId').value = '0';
        byId('modalProductStatus').value = 'ACTIVE';
        byId('modalProductTitle').textContent = 'Add New Product';
        byId('productFormMessage').textContent = '';
        showModal();
    }

    function openEditModal(product) {
        byId('productForm').reset();
        byId('modalAction').value = 'update';
        byId('modalProductId').value = product.productId;
        byId('modalProductName').value = product.productName || '';
        byId('modalProductCategory').value = product.category || '';
        byId('modalProductUnit').value = product.defaultUnit || 'kg';
        byId('modalProductDesc').value = product.description || '';
        byId('modalProductStatus').value = product.status || 'ACTIVE';
        byId('modalProductTitle').textContent = 'Edit Product';
        byId('productFormMessage').textContent = '';
        showModal();
    }

    function showModal() {
        byId('productModal').hidden = false;
        document.body.classList.add('catalog-modal-open');
        window.setTimeout(() => byId('modalProductName').focus(), 0);
    }
    function closeModal() {
        const modal = byId('productModal');
        if (!modal || modal.hidden) return;
        modal.hidden = true;
        document.body.classList.remove('catalog-modal-open');
    }


    async function saveProduct(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const message = byId('productFormMessage');
        if (!form.reportValidity()) return;
        const payload = new FormData();
        payload.append('action', byId('modalAction').value);
        payload.append('productId', byId('modalProductId').value);
        payload.append('productName', byId('modalProductName').value.trim());
        payload.append('category', byId('modalProductCategory').value.trim());
        payload.append('defaultUnit', byId('modalProductUnit').value);
        payload.append('description', byId('modalProductDesc').value.trim());
        payload.append('status', byId('modalProductStatus').value);
        const button = form.querySelector('[type="submit"]');
        button.disabled = true;
        button.textContent = 'Saving…';
        message.textContent = 'Saving product details…';
        message.className = 'catalog-form-message';
        try {
            const response = await fetch('api/products', { method: 'POST', body: payload });
            const result = await response.json();
            if (!response.ok || !result.success) throw new Error(result.message || 'Product could not be saved.');
            closeModal();
            showToast(result.message || 'Product saved successfully.', 'success');
            await loadProducts();
        } catch (error) {
            message.textContent = error.message || 'The product could not be saved. Check your connection and retry.';
            message.className = 'catalog-form-message is-error';
        } finally {
            button.disabled = false;
            button.textContent = 'Save product';
        }
    }

    function showToast(message, type) {
        const toast = document.createElement('div');
        toast.className = `catalog-toast is-${type}`;
        toast.textContent = message;
        byId('catalogToastRegion').append(toast);
        window.setTimeout(() => toast.remove(), 4800);
    }


    function escapeHtml(value) {
        const element = document.createElement('span');
        element.textContent = value == null ? '' : String(value);
        return element.innerHTML.replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }
})();

