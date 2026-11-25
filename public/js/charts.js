/**
 * AgriPrice Rwanda - Data Visualizations (Chart.js)
 * Implements the 3 required interactive agricultural price charts.
 */
if (typeof Chart !== 'undefined') {
    Chart.defaults.color = '#142B4A';
    Chart.defaults.borderColor = 'rgba(20, 43, 74, 0.10)';
    Chart.defaults.font.family = 'Segoe UI, Roboto, Helvetica, Arial, sans-serif';
}

const AgriCharts = {
    trendChartInstance: null,
    comparisonChartInstance: null,
    distributionChartInstance: null,

    // Chart 1: Price Trends (Line Chart)
    async initTrendChart(canvasId, productId = 1, marketId = null, days = 16, statusId = null) {
        const canvas = document.getElementById(canvasId);
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        this.setChartState(statusId, 'Loading price history…');

        try {
            const data = await AgriAPI.getTrendHistory(productId, marketId, days);
            if (!Array.isArray(data)) throw new Error('Trend data response is invalid.');
            if (!data.length) {
                if (this.trendChartInstance) this.trendChartInstance.destroy();
                this.trendChartInstance = null;
                this.setChartState(statusId, 'No past prices are available for this choice.');
                return;
            }
            const labels = data.map(d => d.date);
            const avgPrices = data.map(d => d.avgPrice);
            const minPrices = data.map(d => d.minPrice);
            const maxPrices = data.map(d => d.maxPrice);

            if (this.trendChartInstance) {
                this.trendChartInstance.destroy();
            }

            this.trendChartInstance = new Chart(ctx, {
                type: 'line',
                data: {
                    labels: labels,
                    datasets: [
                        {
                            label: 'Average price',
                            data: avgPrices,
                            borderColor: '#4B5320',
                            backgroundColor: 'rgba(75, 83, 32, 0.12)',
                            borderWidth: 3,
                            fill: true,
                            tension: 0.3,
                            pointRadius: 4,
                            pointBackgroundColor: '#142B4A'
                        },
                        {
                            label: 'Lowest price',
                            data: minPrices,
                            borderColor: '#87CEEB',
                            borderDash: [5, 5],
                            borderWidth: 2,
                            fill: false,
                            tension: 0.2
                        },
                        {
                            label: 'Highest price',
                            data: maxPrices,
                            borderColor: '#DC2626',
                            borderDash: [3, 3],
                            borderWidth: 2,
                            fill: false,
                            tension: 0.2
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { position: 'top' },
                        tooltip: {
                            callbacks: {
                                label: function(context) {
                                    return `${context.dataset.label}: ${context.parsed.y.toLocaleString()} RWF`;
                                }
                            }
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: false,
                            title: { display: true, text: 'Price (RWF / unit)' }
                        },
                        x: {
                            title: { display: true, text: 'Date reported' }
                        }
                    }
                }
            });
            this.setChartState(statusId, `Prices from ${data.length} market days.`);
        } catch (e) {
            console.error('Failed to load Trend Chart:', e);
            this.setChartState(statusId, 'We couldn’t load past prices.', 'error');
        }
    },

    // Chart 2: Market Comparison (Bar Chart)
    async initComparisonChart(canvasId, productId = 1, date = null, statusId = null) {
        const canvas = document.getElementById(canvasId);
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        this.setChartState(statusId, 'Loading market comparison…');

        try {
            const data = await AgriAPI.getMarketComparison(productId, date);
            if (!Array.isArray(data)) throw new Error('Market comparison response is invalid.');
            if (!data.length) {
                if (this.comparisonChartInstance) this.comparisonChartInstance.destroy();
                this.comparisonChartInstance = null;
                this.setChartState(statusId, 'No prices were reported for this product on that date.');
                return;
            }
            const labels = data.map(d => d.marketName);
            const prices = data.map(d => d.price);

            if (this.comparisonChartInstance) {
                this.comparisonChartInstance.destroy();
            }

            this.comparisonChartInstance = new Chart(ctx, {
                type: 'bar',
                data: {
                    labels: labels,
                    datasets: [{
                        label: 'Price (RWF / kg)',
                        data: prices,
                        backgroundColor: [
                            'rgba(135, 206, 235, 0.92)',
                            'rgba(75, 83, 32, 0.88)',
                            'rgba(20, 43, 74, 0.88)',
                            'rgba(98, 179, 216, 0.92)'
                        ],
                        borderColor: '#142B4A',
                        borderWidth: 1.5,
                        borderRadius: 6
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { display: false },
                        tooltip: {
                            callbacks: {
                                label: (ctx) => `${ctx.parsed.y.toLocaleString()} RWF`
                            }
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            title: { display: true, text: 'Price in RWF' }
                        }
                    }
                }
            });
            this.setChartState(statusId, `${data.length} market${data.length === 1 ? '' : 's'} compared.`);
        } catch (e) {
            console.error('Failed to load Comparison Chart:', e);
            this.setChartState(statusId, 'We couldn’t load market prices.', 'error');
        }
    },

    // Chart 3: Market Reporting Distribution (Pie / Doughnut Chart)
    async initDistributionChart(canvasId, statusId = null) {
        const canvas = document.getElementById(canvasId);
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        this.setChartState(statusId, 'Loading prices by market…');

        try {
            const data = await AgriAPI.getMarketDistribution();
            if (!Array.isArray(data)) throw new Error('Distribution response is invalid.');
            if (!data.length) {
                if (this.distributionChartInstance) this.distributionChartInstance.destroy();
                this.distributionChartInstance = null;
                this.setChartState(statusId, 'No prices have been reported yet.');
                return;
            }
            const labels = data.map(d => d.marketName);
            const counts = data.map(d => d.count);

            if (this.distributionChartInstance) {
                this.distributionChartInstance.destroy();
            }

            this.distributionChartInstance = new Chart(ctx, {
                type: 'pie',
                data: {
                    labels: labels,
                    datasets: [{
                        data: counts,
                        backgroundColor: [
                            '#87CEEB',
                            '#4B5320',
                            '#142B4A',
                            '#62B3D8',
                            '#EBE3D3'
                        ],
                        hoverOffset: 6
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { position: 'bottom' }
                    }
                }
            });
            this.setChartState(statusId, `${data.length} markets represented.`);
        } catch (e) {
            console.error('Failed to load Distribution Chart:', e);
            this.setChartState(statusId, 'We couldn’t load prices by market.', 'error');
        }
    },

    setChartState(statusId, message, tone = '') {
        const state = statusId && document.getElementById(statusId);
        if (state) {
            state.textContent = message;
            state.className = `chart-state${tone === 'error' ? ' text-danger' : ''}`;
        }
    }
};
