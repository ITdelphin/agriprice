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
                        <strong>Target Price Reached!</strong> ${alertCount} watched commodity price(s) in Rubavu/Gisenyi markets have met or fallen below your target buy price!
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
                    <p>No agricultural commodities on your watchlist yet.</p>
                    <p class="text-muted">Browse market prices and click "Add to Watchlist" to set target price alerts.</p>
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
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
        `;

        items.forEach(item => {
            const isAlert = item.targetReached;
            const statusClass = isAlert ? 'badge-success' : 'badge-neutral';
            const statusText = isAlert ? 'TARGET REACHED' : 'MONITORING';
            const currentPriceText = item.currentPrice > 0 
                ? `${item.currentPrice.toLocaleString()} RWF / ${item.defaultUnit || 'kg'}`
                : 'Awaiting submission';

            html += `
                <tr class="${isAlert ? 'row-highlight-success' : ''}">
                    <td><strong>${this.escape(item.productName)}</strong></td>
                    <td><span class="badge badge-info">${this.escape(item.category || 'General')}</span></td>
                    <td>${this.escape(item.marketName || 'All Markets')}</td>
                    <td class="price-cell">${currentPriceText}</td>
                    <td>
                        <div class="inline-edit">
                            <input type="number" class="input-target-price" id="target-input-${item.watchlistId}" 
                                   value="${item.targetPrice}" step="10" min="1" style="width: 100px;">
                            <span>RWF</span>
                        </div>
                    </td>
                    <td><span class="badge ${statusClass}">${statusText}</span></td>
                    <td>
                        <button class="btn btn-sm btn-outline btn-save-target" data-id="${item.watchlistId}">Update</button>
                        <button class="btn btn-sm btn-danger btn-remove-item" data-id="${item.watchlistId}">Remove</button>
                    </td>
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
        if (!this.container) return;
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
        if (!this.container) return;
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
