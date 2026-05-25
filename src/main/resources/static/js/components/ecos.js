/**
 * EcosComponent - ECOS 카테고리/지표, 스프레드 계산 (11종)
 * 소유 프로퍼티: ecos
 */
const EcosComponent = {
    ecos: {
        categories: [],
        selectedCategory: null,
        indicators: [],
        loading: false,
        viewMode: 'table',
        historyData: [],
        historyLoading: false,
        _requestGeneration: 0,
        _historyGeneration: 0,
        _historyCache: {},
        _tooltipText: '',
        _rawTooltipText: '',
    },

    initEcosCharts() {
        Object.defineProperty(this.ecos, '_chartInstances', {
            value: new Map(), writable: true, enumerable: false
        });
    },

    async loadEcosCategories() {
        try {
            this.ecos.categories = await API.getEcosCategories() || [];
            if (this.ecos.categories.length > 0 && !this.ecos.selectedCategory) {
                this.ecos.selectedCategory = this.ecos.categories[0].name;
                await this.loadEcosIndicators();
            }
        } catch (e) {
            console.error('ECOS 카테고리 로드 실패:', e);
            this.ecos.categories = [];
        }
    },

    async loadEcosIndicators() {
        if (!this.ecos.selectedCategory) return;

        const thisGeneration = ++this.ecos._requestGeneration;
        this.ecos.loading = true;

        try {
            const result = await API.getEcosIndicators(this.ecos.selectedCategory) || [];
            if (thisGeneration !== this.ecos._requestGeneration) return;
            this.ecos.indicators = result;
        } catch (e) {
            if (thisGeneration !== this.ecos._requestGeneration) return;
            console.error('ECOS 지표 로드 실패:', e);
            this.ecos.indicators = [];
        } finally {
            if (thisGeneration === this.ecos._requestGeneration) {
                this.ecos.loading = false;
            }
        }
    },

    async selectEcosCategory(categoryName) {
        this.ecos.selectedCategory = categoryName;
        this.ecos._tooltipText = '';
        this.ecos._rawTooltipText = '';
        await this.loadEcosIndicators();
        if (this.ecos.viewMode === 'chart') {
            await this.loadEcosHistory();
        }
        // ECONOMIC 모드로 챗봇이 열려 있으면 대화 초기화 + 카테고리 갱신
        if (this.chat.chatMode === 'ECONOMIC' && this.chat.isOpen) {
            if (this.chat._abortController) {
                this.chat._abortController.abort();
            }
            this.chat.messages = [];
            this.chat.indicatorCategory = categoryName;
            this.chat.isLoading = false;
        }
    },

    async switchEcosViewMode(mode) {
        this.ecos.viewMode = mode;
        if (mode === 'chart') {
            await this.loadEcosHistory();
        } else {
            this.destroyEcosCharts();
        }
    },

    async loadEcosHistory() {
        if (!this.ecos.selectedCategory) return;

        const cached = this.ecos._historyCache[this.ecos.selectedCategory];
        if (cached) {
            this.ecos.historyData = cached;
            this.$nextTick(() => this.renderEcosCharts());
            return;
        }

        const thisGeneration = ++this.ecos._historyGeneration;
        this.ecos.historyLoading = true;

        try {
            const result = await API.getEcosIndicatorHistory(this.ecos.selectedCategory) || [];
            if (thisGeneration !== this.ecos._historyGeneration) return;
            this.ecos.historyData = result;

            // LRU 캐시: 최대 3개 카테고리
            const cacheKeys = Object.keys(this.ecos._historyCache);
            if (cacheKeys.length >= 3) {
                delete this.ecos._historyCache[cacheKeys[0]];
            }
            this.ecos._historyCache[this.ecos.selectedCategory] = result;

            this.$nextTick(() => this.renderEcosCharts());
        } catch (e) {
            if (thisGeneration !== this.ecos._historyGeneration) return;
            console.error('ECOS 히스토리 로드 실패:', e);
            this.ecos.historyData = [];
        } finally {
            if (thisGeneration === this.ecos._historyGeneration) {
                this.ecos.historyLoading = false;
            }
        }
    },

    renderEcosCharts() {
        this.destroyEcosCharts();
        if (!this.ecos._chartInstances) this.initEcosCharts();

        this.ecos.historyData.forEach(indicator => {
            if (indicator.history.length < 2) return;

            const canvasId = 'ecos-chart-' + indicator.keystatName.replace(/[^a-zA-Z0-9가-힣]/g, '_');
            const canvas = document.getElementById(canvasId);
            if (!canvas) return;

            const chart = new Chart(canvas, {
                type: 'line',
                data: {
                    labels: indicator.history.map(h => h.cycle),
                    datasets: [{
                        data: indicator.history.map(h => {
                            if (!h.dataValue) return null;
                            const v = parseFloat(h.dataValue.replace(/,/g, ''));
                            return isNaN(v) ? null : v;
                        }),
                        borderColor: '#3B82F6',
                        borderWidth: 1.5,
                        pointRadius: 0,
                        pointHoverRadius: 4,
                        tension: 0.3,
                        fill: false,
                        spanGaps: false,
                    }]
                },
                options: {
                    animation: false,
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { display: false },
                        tooltip: {
                            enabled: true,
                            mode: 'index',
                            intersect: false,
                            callbacks: {
                                label: (ctx) => {
                                    const val = ctx.parsed.y;
                                    return val !== null ? `${val} ${indicator.unitName || ''}` : '데이터 없음';
                                }
                            }
                        },
                    },
                    scales: {
                        x: {
                            display: true,
                            ticks: { maxRotation: 45, autoSkip: true, maxTicksLimit: 8, font: { size: 10 } },
                            grid: { display: false }
                        },
                        y: {
                            display: true,
                            ticks: { maxTicksLimit: 5, font: { size: 10 } }
                        }
                    }
                }
            });
            this.ecos._chartInstances.set(canvasId, chart);
        });
    },

    destroyEcosCharts() {
        if (!this.ecos._chartInstances) return;
        this.ecos._chartInstances.forEach(chart => chart.destroy());
        this.ecos._chartInstances.clear();
    },

    getEcosSortedIndicators() {
        return [...this.ecos.indicators].sort((a, b) =>
            a.className.localeCompare(b.className)
        );
    },

};