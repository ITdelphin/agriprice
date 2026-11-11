/**
 * AgriPrice Rwanda - Data Visualizations (Chart.js)
 * Implements the 3 required interactive agricultural price charts.
 */
const AgriCharts = {
    trendChartInstance: null,
    comparisonChartInstance: null,
    distributionChartInstance: null,

    // Chart 1: Price Trends (Line Chart)
    async initTrendChart(canvasId, productId = 1, marketId = null) {
        const canvas = document.getElementById(canvasId);
        if (!canvas) return;
        const ctx = canvas.getContext('2d');

        try {
            const data = await AgriAPI.getTrendHistory(productId, marketId, 16);
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
                            label: 'Average Price (RWF)',
                            data: avgPrices,
                            borderColor: '#2e7d32',
                            backgroundColor: 'rgba(46, 125, 50, 0.1)',
                            borderWidth: 3,
                            fill: true,
                            tension: 0.3,
                            pointRadius: 4,
                            pointBackgroundColor: '#1b5e20'
                        },
                        {
                            label: 'Min Price (RWF)',
                            data: minPrices,
                            borderColor: '#81c784',
                            borderDash: [5, 5],
                            borderWidth: 2,
                            fill: false,
                            tension: 0.2
                        },
                        {
                            label: 'Max Price (RWF)',
                            data: maxPrices,
                            borderColor: '#e65100',
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
                            title: { display: true, text: 'Survey Date' }
                        }
                    }
                }
            });
        } catch (e) {
            console.error('Failed to load Trend Chart:', e);
        }
    },

    // Chart 2: Market Comparison (Bar Chart)
    async initComparisonChart(canvasId, productId = 1, date = '2026-11-09') {
        const canvas = document.getElementById(canvasId);
        if (!canvas) return;
        const ctx = canvas.getContext('2d');

        try {
            const data = await AgriAPI.getMarketComparison(productId, date);
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
                            'rgba(46, 125, 50, 0.85)',
                            'rgba(56, 142, 60, 0.85)',
                            'rgba(76, 175, 80, 0.85)',
                            'rgba(129, 199, 132, 0.85)'
                        ],
                        borderColor: '#1b5e20',
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
        } catch (e) {
            console.error('Failed to load Comparison Chart:', e);
        }
    },

    // Chart 3: Market Reporting Distribution (Pie / Doughnut Chart)
    async initDistributionChart(canvasId) {
        const canvas = document.getElementById(canvasId);
        if (!canvas) return;
        const ctx = canvas.getContext('2d');

        try {
            const data = await AgriAPI.getMarketDistribution();
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
                            '#2e7d32',
                            '#43a047',
                            '#66bb6a',
                            '#81c784'
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
        } catch (e) {
            console.error('Failed to load Distribution Chart:', e);
        }
    }
};
