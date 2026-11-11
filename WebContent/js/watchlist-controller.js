/**
 * AgriPrice Rwanda - Watchlist Client-Side MVC: CONTROLLER
 * Strict Requirement: Coordinates Model and View, manages API I/O, validates input.
 */
class WatchlistController {
    constructor(model, view) {
        this.model = model;
        this.view = view;

        // Register model observer
        this.model.subscribe((items, alertCount) => {
            this.view.render(items, alertCount);
        });

        // Bind DOM events from View
        this.view.bindUpdateTarget(this.handleUpdateTarget.bind(this));
        this.view.bindRemoveItem(this.handleRemoveItem.bind(this));

        // Load initial watchlist from database
        this.loadWatchlist();
    }

    async loadWatchlist() {
        try {
            const sessionId = this.model.visitorSessionId;
            const res = await fetch(`api/watchlist?visitorSessionId=${encodeURIComponent(sessionId)}`);
            if (res.ok) {
                const data = await res.json();
                if (data.visitorSessionId) {
                    this.model.setVisitorSessionId(data.visitorSessionId);
                }
                this.model.setItems(data.items || []);
            } else {
                console.warn('Watchlist fetch returned status', res.status);
            }
        } catch (err) {
            console.error('Failed to load watchlist from backend API:', err);
        }
    }

    async handleAddItem(productId, marketId, targetPrice) {
        if (!productId || isNaN(productId)) {
            this.view.showToast('Invalid product selected.', 'error');
            return;
        }
        if (!targetPrice || isNaN(targetPrice) || targetPrice <= 0) {
            this.view.showToast('Target price must be greater than 0 RWF.', 'error');
            return;
        }

        try {
            const payload = {
                action: 'add',
                visitorSessionId: this.model.visitorSessionId,
                productId: parseInt(productId, 10),
                marketId: marketId ? parseInt(marketId, 10) : null,
                targetPrice: parseFloat(targetPrice)
            };

            const res = await fetch('api/watchlist', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const result = await res.json();
            if (res.ok && result.success) {
                this.view.showToast(result.message || 'Added to watchlist successfully!');
                await this.loadWatchlist(); // Reload updated items with joined data
            } else {
                this.view.showToast(result.message || 'Failed to add item.', 'error');
            }
        } catch (err) {
            this.view.showToast('Network error while saving watchlist.', 'error');
            console.error(err);
        }
    }

    async handleUpdateTarget(watchlistId, newTargetPrice) {
        if (!newTargetPrice || isNaN(newTargetPrice) || newTargetPrice <= 0) {
            this.view.showToast('Target price must be a positive number.', 'error');
            return;
        }

        try {
            const payload = {
                action: 'update',
                visitorSessionId: this.model.visitorSessionId,
                watchlistId: watchlistId,
                targetPrice: newTargetPrice
            };

            const res = await fetch('api/watchlist', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const result = await res.json();
            if (res.ok && result.success) {
                this.model.updateTargetPrice(watchlistId, newTargetPrice);
                this.view.showToast('Target price updated.');
            } else {
                this.view.showToast(result.message || 'Update failed.', 'error');
            }
        } catch (err) {
            this.view.showToast('Network error during update.', 'error');
        }
    }

    async handleRemoveItem(watchlistId) {
        try {
            const payload = {
                action: 'delete',
                visitorSessionId: this.model.visitorSessionId,
                watchlistId: watchlistId
            };

            const res = await fetch('api/watchlist', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const result = await res.json();
            if (res.ok && result.success) {
                this.model.removeItem(watchlistId);
                this.view.showToast('Item removed from watchlist.');
            } else {
                this.view.showToast(result.message || 'Could not delete item.', 'error');
            }
        } catch (err) {
            this.view.showToast('Network error removing item.', 'error');
        }
    }
}

if (typeof module !== 'undefined' && module.exports) {
    module.exports = WatchlistController;
}
