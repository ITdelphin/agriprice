/* Product catalog page: AJAX data, filters, paging and agent-only CRUD. */
(function () {
    'use strict';

    const pageSize = 8;
    const state = { products: [], filtered: [], page: 1, isAgent: false, currentUser: null, loading: false };
    const byId = id => document.getElementById(id);

    function productImage(product) {
        const uploaded = product && product.imageUrl && product.imageUrl !== 'NULL' ? String(product.imageUrl) : '';
        if (uploaded) return uploaded;
        const value = `${(product && product.productName) || ''} ${(product && product.category) || ''}`.toLowerCase();
        if (value.includes('sweet') && value.includes('potato')) return 'assets/images/catalog/sweet-potatoes.jpg';
        if (value.includes('potato') || value.includes('irish')) return 'assets/images/catalog/potatoes.jpg';
        if (value.includes('bean') || value.includes('pea') || value.includes('legume')) return 'assets/images/catalog/beans.jpg';
        if (value.includes('cassava') || value.includes('manioc')) return 'assets/images/catalog/cassava.jpg';
        if (value.includes('onion')) return 'assets/images/catalog/onions.jpg';
        if (value.includes('banana') || value.includes('plantain')) return 'assets/images/catalog/bananas.jpg';
        if (value.includes('maize') || value.includes('corn') || value.includes('cereal') || value.includes('grain')) return 'assets/images/catalog/maize.jpg';
        return 'assets/images/catalog/product-placeholder.svg';
    }

    document.addEventListener('DOMContentLoaded', init);

    async function init() {
        document.addEventListener('keydown', event => { if (event.key === 'Escape') closeModal(); });
        bindFilters();
        bindModal();
        try {
            const auth = await AgriAPI.checkAuthStatus();
            state.isAgent = Boolean(auth && auth.authenticated);
            state.currentUser = auth && auth.authenticated ? auth : null;
        } catch (_) {
            state.isAgent = false;
        }
        const addProductButton = byId('btnOpenAddProduct');
        if (addProductButton) addProductButton.style.display = state.isAgent ? 'inline-flex' : 'none';
        if (!state.isAgent) {
            const modal = byId('productModal');
            if (modal) modal.remove();
        }
        await loadProducts();
    }


    function bindFilters() {
        byId('productFilterForm').addEventListener('submit', event => { event.preventDefault(); applyFilters(); });
        byId('productSearchInput').addEventListener('input', applyFilters);
        byId('productCategoryFilter').addEventListener('change', applyFilters);
        byId('productStatusFilter').addEventListener('change', applyFilters);
        byId('clearProductFilters').addEventListener('click', () => {
            byId('productFilterForm').reset();
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
            tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; padding: 40px;"><strong>${state.products.length ? 'No products found' : 'No products yet'}</strong><br><span class="text-muted">${state.products.length ? 'Try another name or clear the filters.' : 'Once an agent adds a product, it will appear here.'}</span>${state.products.length ? '<br><button type="button" id="emptyClearFilters" class="btn btn-sm btn-secondary" style="margin-top: 12px;">Clear filters</button>' : ''}</td></tr>`;
            byId('productPaginationInfo').textContent = 'Nothing to show right now';
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
            ? `<div class="row-actions"><button type="button" class="btn btn-sm btn-outline" data-action="edit" data-product-id="${id}">Edit</button><button type="button" class="btn btn-sm btn-secondary" data-action="delete" data-product-id="${id}">Delete</button></div>`
            : `<a class="btn btn-sm btn-primary" href="prices.html?productId=${encodeURIComponent(id)}">See prices</a>`;
        return `<tr>
            <td>${String(product.productId || rowNumber).padStart(3, '0')}</td>
            <td><div class="catalog-product-photo"><img src="${escapeHtml(productImage(product))}" alt="${escapeHtml(name)}" loading="lazy" onerror="this.onerror=null;this.src='assets/images/catalog/product-placeholder.svg'"></div></td>
            <td><strong>${escapeHtml(name)}</strong><br><span class="text-muted">ID ${String(id).padStart(4, '0')}</span></td>
            <td><span class="badge badge-info">${escapeHtml(product.category || 'Uncategorized')}</span></td>
            <td>${escapeHtml(product.defaultUnit || '—')}</td>
            <td><span class="text-muted">${escapeHtml(product.description || 'No description yet')}</span></td>
            <td><span class="badge ${status === 'ACTIVE' ? 'badge-success' : 'badge-neutral'}">${status === 'ACTIVE' ? 'Active' : 'Inactive'}</span></td>
            <td>${actionMarkup}</td>
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
        byId('productsTableBody').innerHTML = '<tr><td colspan="7" style="text-align: center; padding: 40px;">Gathering products…<br><span class="text-muted">This will only take a moment.</span></td></tr>';
        byId('productsCount').textContent = 'Loading…';
        byId('catalogHeroCount').textContent = 'Loading products…';
    }

    function renderError(message) {
        byId('productsTableBody').innerHTML = `<tr><td colspan="7" style="text-align: center; padding: 40px;"><strong>Products could not load</strong><br><span class="text-muted">${escapeHtml(message)}</span><br><button type="button" id="retryProductLoad" class="btn btn-sm btn-primary" style="margin-top: 12px;">Try again</button></td></tr>`;
        byId('productsCount').textContent = 'Temporarily unavailable';
        byId('catalogHeroCount').textContent = 'Connection unavailable';
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
        byId('modalProductTitle').textContent = 'Add product';
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
        byId('modalProductTitle').textContent = 'Edit product';
        byId('productFormMessage').textContent = '';
        showModal();
    }

    function showModal() {
        if (!state.isAgent) return;
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
        if (!state.isAgent) {
            message.textContent = 'Only signed-in agents can add products.';
            message.className = 'catalog-form-message is-error';
            return;
        }
        if (!form.reportValidity()) return;
        const payload = new FormData();
        payload.append('action', byId('modalAction').value);
        payload.append('productId', byId('modalProductId').value);
        payload.append('productName', byId('modalProductName').value.trim());
        payload.append('category', byId('modalProductCategory').value.trim());
        payload.append('defaultUnit', byId('modalProductUnit').value);
        payload.append('description', byId('modalProductDesc').value.trim());
        payload.append('status', byId('modalProductStatus').value);
        const imageInput = byId('modalProductImage');
        if (imageInput && imageInput.files && imageInput.files.length > 0) payload.append('image', imageInput.files[0]);
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

