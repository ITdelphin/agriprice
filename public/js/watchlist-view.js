/**
 * AgriPrice Rwanda - Watchlist Client-Side MVC: VIEW
 * Strict Requirement: DOM rendering, visual updates, toast notifications, and alerts.
 */
class WatchlistView {
    constructor() {
        this.container = document.getElementById('watchlistContainer');
        this.alertBanner = document.getElementById('watchlistAlertBanner');
        this.alertCountBadge = document.getElementById('watchlistBadge');
        this.emptyState = document.getElementById('watchlistEmpty');
        this.canManage = false;
    }

    setCanManage(canManage) {
        this.canManage = Boolean(canManage);
    }

    render(items, alertCount) {
        if (!this.container) return;

        // Update alert counter badge
        if (this.alertCountBadge) {
            this.alertCountBadge.textContent = alertCount;
            this.alertCountBadge.style.display = alertCount > 0 ? 'inline-block' : 'none';
        }

        // Update alert banner
        if (this.alertBanner) {
            if (alertCount > 0) {
                this.alertBanner.style.display = 'block';
                this.alertBanner.innerHTML = `
                    <div class="alert-box alert-success">
                        <strong>Good news —</strong> ${alertCount} prices you’re watching have reached your target or gone lower.
                    </div>
                `;
            } else {
                this.alertBanner.style.display = 'none';
            }
        }

        // Render Empty State
        if (!items || items.length === 0) {
            this.container.innerHTML = `
                <div class="empty-card">
                    <p>You haven’t added any products to watch yet.</p>
                    <p class="text-muted">${this.canManage ? 'Add products and target prices to track them here.' : 'You can view this watchlist. A market agent can add and manage target prices.'}</p>
                </div>
            `;
            return;
        }

        // Render Table
        let html = `
            <table class="data-table">
                <thead>
                    <tr>
                        <th>Product</th>
                        <th>Category</th>
                        <th>Market</th>
                        <th>Current Price</th>
                        <th>Target Price</th>
                        <th>Status</th>
                    <th>${this.canManage ? 'Actions' : 'Access'}</th>
                    </tr>
                </thead>
                <tbody>
        `;

        items.forEach(item => {
            const isAlert = item.targetReached;
            const statusClass = isAlert ? 'badge-success' : 'badge-neutral';
            const statusText = isAlert ? 'Target reached' : 'Watching';
            const currentPriceText = item.currentPrice > 0 
                ? `${item.currentPrice.toLocaleString()} RWF / ${item.defaultUnit || 'kg'}`
                : 'No price reported yet';

            html += `
                <tr class="${isAlert ? 'row-highlight-success' : ''}">
                    <td><strong>${this.escape(item.productName)}</strong></td>
                    <td><span class="badge badge-info">${this.escape(item.category || 'General')}</span></td>
                    <td>${this.escape(item.marketName || 'All Markets')}</td>
                    <td class="price-cell">${currentPriceText}</td>
                    <td>${this.canManage
                        ? `<div class="inline-edit"><input type="number" class="input-target-price" id="target-input-${item.watchlistId}" value="${item.targetPrice}" step="10" min="1" aria-label="Target price for ${this.escape(item.productName)}"><span>RWF</span></div>`
                        : `<span class="watchlist-readonly-price">${Number(item.targetPrice).toLocaleString()} RWF / ${this.escape(item.defaultUnit || 'kg')}</span>`}</td>
                    <td><span class="badge ${statusClass}">${statusText}</span></td>
                    <td>${this.canManage
                        ? `<div class="watchlist-agent-actions"><button class="btn btn-sm btn-outline btn-save-target" data-id="${item.watchlistId}">Save</button><button class="btn btn-sm btn-danger btn-remove-item" data-id="${item.watchlistId}">Delete</button></div>`
                        : '<span class="watchlist-view-only">Viewing only</span>'}</td>
                </tr>
            `;
        });

        html += `
                </tbody>
            </table>
        `;
        this.container.innerHTML = html;
    }

    bindUpdateTarget(handler) {
        if (!this.container || !this.canManage) return;
        this.container.addEventListener('click', e => {
            if (e.target && e.target.classList.contains('btn-save-target')) {
                const id = parseInt(e.target.getAttribute('data-id'), 10);
                const input = document.getElementById(`target-input-${id}`);
                if (input) {
                    const newPrice = parseFloat(input.value);
                    handler(id, newPrice);
                }
            }
        });
    }

    bindRemoveItem(handler) {
        if (!this.container || !this.canManage) return;
        this.container.addEventListener('click', e => {
            if (e.target && e.target.classList.contains('btn-remove-item')) {
                const id = parseInt(e.target.getAttribute('data-id'), 10);
                if (confirm('Are you sure you want to remove this product from your watchlist?')) {
                    handler(id);
                }
            }
        });
    }

    showToast(message, type = 'success') {
        let toastContainer = document.getElementById('toastContainer');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'toastContainer';
            toastContainer.className = 'toast-container';
            document.body.appendChild(toastContainer);
        }
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.textContent = message;
        toastContainer.appendChild(toast);
        setTimeout(() => toast.remove(), 4000);
    }

    escape(str) {
        if (!str) return '';
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }
}

if (typeof module !== 'undefined' && module.exports) {
    module.exports = WatchlistView;
}
